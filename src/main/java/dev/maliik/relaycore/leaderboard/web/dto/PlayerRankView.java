package dev.maliik.relaycore.leaderboard.web.dto;

import java.util.UUID;

/** A single player's standing in a season: their 1-based rank and current score. */
public record PlayerRankView(UUID playerId, long rank, long score) {
}
