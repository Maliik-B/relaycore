package dev.maliik.relaycore.matchmaking.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.maliik.relaycore.common.error.InvalidMatchResultException;
import dev.maliik.relaycore.common.error.MatchAlreadyCompletedException;
import dev.maliik.relaycore.common.error.ResourceNotFoundException;
import dev.maliik.relaycore.common.event.MatchCompleted;
import dev.maliik.relaycore.common.event.Topics;
import dev.maliik.relaycore.common.outbox.OutboxAppender;
import dev.maliik.relaycore.matchmaking.domain.Match;
import dev.maliik.relaycore.matchmaking.domain.MatchParticipant;
import dev.maliik.relaycore.matchmaking.domain.MatchStatus;
import dev.maliik.relaycore.matchmaking.repository.MatchParticipantRepository;
import dev.maliik.relaycore.matchmaking.repository.MatchRepository;
import dev.maliik.relaycore.matchmaking.web.dto.MatchView;

/**
 * The completion path of a match — the event <i>source</i> of the reward loop. Reporting a result
 * transitions an ACTIVE match (formed by the worker) to COMPLETED and writes a {@code match-completed}
 * outbox event in the <b>same transaction</b> as the state change — the dual-write the outbox exists to
 * make atomic. The relay publishes the event; inventory grants the reward and leaderboards rank the winner.
 */
@Service
public class MatchService {

    private final MatchRepository matches;
    private final MatchParticipantRepository participants;
    private final OutboxAppender outboxAppender;

    public MatchService(MatchRepository matches, MatchParticipantRepository participants,
            OutboxAppender outboxAppender) {
        this.matches = matches;
        this.participants = participants;
        this.outboxAppender = outboxAppender;
    }

    @Transactional
    public MatchView reportResult(UUID matchId, UUID winnerId) {
        Match match = matches.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("match", matchId.toString()));
        if (match.getStatus() != MatchStatus.ACTIVE) {
            throw new MatchAlreadyCompletedException(matchId);
        }
        List<UUID> participantIds = participantIds(matchId);
        if (!participantIds.contains(winnerId)) {
            throw new InvalidMatchResultException(matchId, winnerId);
        }

        Instant now = Instant.now();
        match.complete(winnerId, now);

        MatchCompleted event = new MatchCompleted(UUID.randomUUID(), matchId, winnerId, participantIds, now);
        outboxAppender.append("match", matchId.toString(),
                MatchCompleted.class.getSimpleName(), Topics.MATCH_COMPLETED, event);

        return MatchView.of(match, participantIds);
    }

    @Transactional(readOnly = true)
    public MatchView get(UUID matchId) {
        Match match = matches.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("match", matchId.toString()));
        return MatchView.of(match, participantIds(matchId));
    }

    private List<UUID> participantIds(UUID matchId) {
        return participants.findByMatchId(matchId).stream()
                .map(MatchParticipant::getPlayerId)
                .toList();
    }
}
