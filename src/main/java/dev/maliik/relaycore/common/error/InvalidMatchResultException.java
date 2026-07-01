package dev.maliik.relaycore.common.error;

import java.util.UUID;

/**
 * Thrown when a reported winner is not one of the match's participants. A well-formed request the match
 * state can't accept — mapped to HTTP 422 (retrying as-is won't help).
 */
public class InvalidMatchResultException extends RuntimeException {

    public InvalidMatchResultException(UUID matchId, UUID winnerId) {
        super("winner %s is not a participant of match %s".formatted(winnerId, matchId));
    }
}
