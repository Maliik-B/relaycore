package dev.maliik.relaycore.players.service;

import dev.maliik.relaycore.players.web.dto.PlayerProfile;

/**
 * Outcome of a successful register/login: the issued token, its lifetime, and the player's profile.
 * A service-layer carrier that the web layer maps onto its response shape.
 */
public record AuthResult(String token, long expiresInSeconds, PlayerProfile player) {
}
