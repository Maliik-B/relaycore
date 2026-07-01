package dev.maliik.relaycore.matchmaking.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import dev.maliik.relaycore.matchmaking.domain.Match;
import dev.maliik.relaycore.matchmaking.domain.MatchParticipant;
import dev.maliik.relaycore.matchmaking.domain.Ticket;
import dev.maliik.relaycore.matchmaking.domain.TicketStatus;
import dev.maliik.relaycore.matchmaking.repository.MatchParticipantRepository;
import dev.maliik.relaycore.matchmaking.repository.MatchRepository;
import dev.maliik.relaycore.matchmaking.repository.TicketRepository;

/**
 * The background half of matchmaking: on a fixed schedule, drain the open queue and form matches by
 * grouping QUEUED tickets on ({@code region}, {@code skillBucket}). Each full bucket of {@value #MATCH_SIZE}
 * becomes an ACTIVE {@link Match} with its participants, and the consumed tickets are flipped to MATCHED —
 * all in one transaction, so a formed match and its tickets commit together or not at all.
 *
 * <p>Single-threaded like the outbox relay (the default scheduler runs one @Scheduled at a time), so a
 * tick never races itself; a partial ticket bucket simply waits for the next tick's arrivals. A bucket
 * matcher, deliberately — this module's depth is being the event source, not matchmaking sophistication.
 */
@Component
public class MatchmakingWorker {

    private static final Logger log = LoggerFactory.getLogger(MatchmakingWorker.class);

    /** Players per match. Fixed at 2 for M4 (the simplest, most readable bucket). */
    private static final int MATCH_SIZE = 2;

    /** Cap on tickets examined per tick — bounds the unit of work as the queue grows. */
    private static final int MAX_TICKETS_PER_TICK = 100;

    private final TicketRepository tickets;
    private final MatchRepository matches;
    private final MatchParticipantRepository participants;

    public MatchmakingWorker(TicketRepository tickets, MatchRepository matches,
            MatchParticipantRepository participants) {
        this.tickets = tickets;
        this.matches = matches;
        this.participants = participants;
    }

    @Scheduled(fixedDelayString = "${relaycore.matchmaking.poll-interval-ms:1000}")
    @Transactional
    public void formMatches() {
        List<Ticket> queued = tickets.findByStatusOrderByCreatedAtAsc(
                TicketStatus.QUEUED, PageRequest.of(0, MAX_TICKETS_PER_TICK));
        if (queued.isEmpty()) {
            return;
        }
        for (Map.Entry<BucketKey, List<Ticket>> bucket : groupByBucket(queued).entrySet()) {
            List<Ticket> waiting = bucket.getValue();
            for (int i = 0; i + MATCH_SIZE <= waiting.size(); i += MATCH_SIZE) {
                form(bucket.getKey(), waiting.subList(i, i + MATCH_SIZE));
            }
        }
    }

    private Map<BucketKey, List<Ticket>> groupByBucket(List<Ticket> queued) {
        Map<BucketKey, List<Ticket>> byBucket = new LinkedHashMap<>();
        for (Ticket ticket : queued) {
            byBucket.computeIfAbsent(new BucketKey(ticket.getRegion(), ticket.getSkillBucket()),
                    key -> new ArrayList<>()).add(ticket);
        }
        return byBucket;
    }

    private void form(BucketKey bucket, List<Ticket> group) {
        Match match = matches.save(Match.active(bucket.region(), bucket.skillBucket()));
        for (Ticket ticket : group) {
            participants.save(MatchParticipant.of(match.getId(), ticket.getPlayerId()));
            ticket.markMatched(match.getId());
        }
        log.info("Formed match {} (region {}, bucket {}) from {} tickets",
                match.getId(), bucket.region(), bucket.skillBucket(), group.size());
    }

    /** Grouping key: matches only form among tickets sharing both region and skill bucket. */
    private record BucketKey(String region, int skillBucket) {
    }
}
