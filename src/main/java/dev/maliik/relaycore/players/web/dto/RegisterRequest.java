package dev.maliik.relaycore.players.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Registration payload. The password length is capped at 72 bytes — BCrypt silently truncates
 * beyond that, so rejecting it up front avoids a surprising auth footgun.
 */
public record RegisterRequest(
        @NotBlank
        @Size(min = 3, max = 32)
        @Pattern(regexp = "^[A-Za-z0-9_]+$", message = "must contain only letters, digits, or underscores")
        String username,

        @NotBlank
        @Email
        @Size(max = 255)
        String email,

        @NotBlank
        @Size(min = 8, max = 72)
        String password) {
}
