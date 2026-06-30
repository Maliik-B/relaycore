package dev.maliik.relaycore.common.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Event contract: a match has finished. Produced by matchmaking (via the outbox) onto
 * {@link Topics#MATCH_COMPLETED}. The {@code eventId} is the de-duplication handle — consumers use it
 * as an idempotency key so a Kafka redelivery applies its effect exactly once.
 */
public record MatchCompleted(UUID eventId, UUID matchId, UUID winnerId, Instant completedAt) {
}
