package dev.maliik.relaycore.common.error;

import java.util.UUID;

/**
 * Thrown when a result is reported for a match that is no longer ACTIVE (already completed). A conflict
 * with the current state — mapped to HTTP 409.
 */
public class MatchAlreadyCompletedException extends RuntimeException {

    public MatchAlreadyCompletedException(UUID matchId) {
        super("match already completed: " + matchId);
    }
}
