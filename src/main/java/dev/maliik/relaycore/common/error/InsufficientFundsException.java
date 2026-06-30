package dev.maliik.relaycore.common.error;

import java.util.UUID;

/**
 * Thrown when a debit would overdraw a wallet. A well-formed request the current balance can't
 * satisfy — mapped to HTTP 422 (not a transient conflict, so retrying as-is won't help).
 */
public class InsufficientFundsException extends RuntimeException {

    public InsufficientFundsException(UUID playerId, long required, long available) {
        super("insufficient funds for player %s: required %d, available %d".formatted(playerId, required, available));
    }
}
