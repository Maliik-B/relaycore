package dev.maliik.relaycore.common.error;

/**
 * Thrown when an idempotency key is reused for a request whose payload differs from the original.
 * Mapped to HTTP 409 — the key is bound to its first request and can't be repurposed.
 */
public class IdempotencyConflictException extends RuntimeException {

    public IdempotencyConflictException(String scope, String key) {
        super("idempotency key reused with a different request (%s): %s".formatted(scope, key));
    }
}
