package dev.maliik.relaycore.players.web.dto;

import dev.maliik.relaycore.players.service.AuthResult;

/**
 * Auth endpoint response: a bearer access token plus the caller's profile.
 */
public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        PlayerProfile player) {

    public static AuthResponse from(AuthResult result) {
        return new AuthResponse(result.token(), "Bearer", result.expiresInSeconds(), result.player());
    }
}
