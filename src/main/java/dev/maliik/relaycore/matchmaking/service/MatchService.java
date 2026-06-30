package dev.maliik.relaycore.matchmaking.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.maliik.relaycore.common.error.DuplicateResourceException;
import dev.maliik.relaycore.common.error.ResourceNotFoundException;
import dev.maliik.relaycore.common.event.MatchCompleted;
import dev.maliik.relaycore.common.event.Topics;
import dev.maliik.relaycore.common.outbox.OutboxAppender;
import dev.maliik.relaycore.matchmaking.domain.Match;
import dev.maliik.relaycore.matchmaking.repository.MatchRepository;
import dev.maliik.relaycore.matchmaking.web.dto.MatchView;

/**
 * The event <i>source</i> of the reward loop. Recording a match result writes the {@code matches} row
 * AND a {@code match-completed} outbox event in <b>one transaction</b> — the dual-write the outbox
 * exists to make atomic. The relay publishes the event; inventory consumes it and grants the reward.
 */
@Service
public class MatchService {

    private final MatchRepository matches;
    private final OutboxAppender outboxAppender;

    public MatchService(MatchRepository matches, OutboxAppender outboxAppender) {
        this.matches = matches;
        this.outboxAppender = outboxAppender;
    }

    @Transactional
    public MatchView reportResult(UUID matchId, UUID winnerId) {
        if (matches.existsById(matchId)) {
            throw new DuplicateResourceException("match result", matchId.toString());
        }
        Instant now = Instant.now();
        Match match = matches.save(Match.completed(matchId, winnerId, now));

        MatchCompleted event = new MatchCompleted(UUID.randomUUID(), matchId, winnerId, now);
        outboxAppender.append("match", matchId.toString(),
                MatchCompleted.class.getSimpleName(), Topics.MATCH_COMPLETED, event);

        return MatchView.from(match);
    }

    @Transactional(readOnly = true)
    public MatchView get(UUID matchId) {
        return matches.findById(matchId)
                .map(MatchView::from)
                .orElseThrow(() -> new ResourceNotFoundException("match", matchId.toString()));
    }
}
