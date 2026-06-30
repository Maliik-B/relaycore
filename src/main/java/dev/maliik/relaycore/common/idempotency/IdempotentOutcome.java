package dev.maliik.relaycore.common.idempotency;

/**
 * Result of an idempotent operation: the value plus whether it came from a stored replay rather than
 * a fresh execution. Lets the web layer signal replays (e.g. an {@code Idempotency-Replayed} header).
 */
public record IdempotentOutcome<T>(T result, boolean replayed) {
}
