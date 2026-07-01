package dev.maliik.relaycore.leaderboard.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.maliik.relaycore.common.event.MatchCompleted;
import dev.maliik.relaycore.common.event.Topics;
import dev.maliik.relaycore.leaderboard.service.LeaderboardService;

/**
 * Consumes {@link Topics#MATCH_COMPLETED} on its <b>own consumer group</b> ({@code relaycore-leaderboard})
 * so it fans out from the same topic independently of the inventory consumer — the pub/sub decoupling
 * microservices rely on, done inside the monolith. Each match win increments the winner's season score;
 * idempotency (against Kafka redelivery) lives in {@link LeaderboardService#recordWin}, keyed on the event id.
 */
@Component
public class LeaderboardListener {

    private static final Logger log = LoggerFactory.getLogger(LeaderboardListener.class);

    private final LeaderboardService leaderboardService;
    private final ObjectMapper objectMapper;

    public LeaderboardListener(LeaderboardService leaderboardService, ObjectMapper objectMapper) {
        this.leaderboardService = leaderboardService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = Topics.MATCH_COMPLETED, groupId = "relaycore-leaderboard")
    public void onMatchCompleted(String payload) {
        MatchCompleted event = parse(payload);
        log.info("Ranking winner {} for match {} in season {} (event {})",
                event.winnerId(), event.matchId(), LeaderboardService.DEFAULT_SEASON, event.eventId());
        leaderboardService.recordWin(LeaderboardService.DEFAULT_SEASON, event.winnerId(), event.eventId());
    }

    private MatchCompleted parse(String payload) {
        try {
            return objectMapper.readValue(payload, MatchCompleted.class);
        } catch (Exception ex) {
            throw new IllegalStateException("Unparseable match-completed payload: " + payload, ex);
        }
    }
}
