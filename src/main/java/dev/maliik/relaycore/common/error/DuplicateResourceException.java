package dev.maliik.relaycore.common.error;

/**
 * Thrown when a uniqueness constraint would be violated (e.g. a taken username or email).
 * Mapped to HTTP 409.
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String field, String value) {
        super("%s already in use: %s".formatted(field, value));
    }
}
