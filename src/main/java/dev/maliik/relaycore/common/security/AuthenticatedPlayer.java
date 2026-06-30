package dev.maliik.relaycore.common.security;

import java.util.UUID;

/**
 * The authenticated principal carried in the security context for a request that presented a valid
 * JWT. Deliberately minimal — just the identity claims from the token, no database lookup per request.
 */
public record AuthenticatedPlayer(UUID id, String username) {
}
