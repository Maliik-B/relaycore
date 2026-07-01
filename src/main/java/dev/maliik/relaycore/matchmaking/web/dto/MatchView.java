package dev.maliik.relaycore.matchmaking.web.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import dev.maliik.relaycore.matchmaking.domain.Match;

/** Read model for a match. {@code winnerId} and {@code completedAt} are null until the result is reported. */
public record MatchView(UUID id, String status, String region, int skillBucket, UUID winnerId,
        List<UUID> participantIds, Instant completedAt) {

    public static MatchView of(Match match, List<UUID> participantIds) {
        return new MatchView(match.getId(), match.getStatus().name(), match.getRegion(), match.getSkillBucket(),
                match.getWinnerId(), participantIds, match.getCompletedAt());
    }
}
