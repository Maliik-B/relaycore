package dev.maliik.relaycore.matchmaking.web.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

/**
 * Result report for a match: who won. Kept deliberately thin for M3 — full participant lists and
 * scoring land with the matchmaking lifecycle in M4.
 */
public record MatchResultRequest(@NotNull UUID winnerId) {
}
