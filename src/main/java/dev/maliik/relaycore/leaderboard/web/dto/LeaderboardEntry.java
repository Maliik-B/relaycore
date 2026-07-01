package dev.maliik.relaycore.leaderboard.web.dto;

import java.util.UUID;

/** One row of a leaderboard page: a player's 1-based rank and score. */
public record LeaderboardEntry(long rank, UUID playerId, long score) {
}
