package dev.maliik.relaycore.common.error;

/**
 * Thrown when a requested resource (a player, an item, ...) does not exist. Mapped to HTTP 404.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resource, String id) {
        super("%s not found: %s".formatted(resource, id));
    }
}
