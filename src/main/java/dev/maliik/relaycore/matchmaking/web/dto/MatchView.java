package dev.maliik.relaycore.matchmaking.web.dto;

import java.time.Instant;
import java.util.UUID;

import dev.maliik.relaycore.matchmaking.domain.Match;

/** Read model for a match. */
public record MatchView(UUID id, String status, UUID winnerId, Instant completedAt) {

    public static MatchView from(Match match) {
        return new MatchView(match.getId(), match.getStatus().name(), match.getWinnerId(), match.getCompletedAt());
    }
}
