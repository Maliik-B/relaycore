package dev.maliik.relaycore.players.web.dto;

import java.time.Instant;
import java.util.UUID;

import dev.maliik.relaycore.players.domain.Player;

/**
 * Public read model for a player profile — the cached, serialized representation returned by the
 * read endpoints. Excludes credentials.
 */
public record PlayerProfile(
        UUID id,
        String username,
        String displayName,
        int matchesPlayed,
        int wins,
        int losses,
        Instant createdAt) {

    public static PlayerProfile from(Player player) {
        return new PlayerProfile(
                player.getId(),
                player.getUsername(),
                player.getDisplayName(),
                player.getStats().getMatchesPlayed(),
                player.getStats().getWins(),
                player.getStats().getLosses(),
                player.getCreatedAt());
    }
}
