package dev.maliik.relaycore.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;

class JwtServiceTest {

    private static final String SECRET = "test-secret-test-secret-test-secret-0123456789";

    private final JwtProperties properties = new JwtProperties(SECRET, Duration.ofHours(1));
    private final JwtService jwtService = new JwtService(properties);

    @Test
    void issuesTokenThatParsesBackToTheSameClaims() {
        UUID playerId = UUID.randomUUID();

        String token = jwtService.issue(playerId, "neo");
        Claims claims = jwtService.parse(token).getPayload();

        assertThat(claims.getSubject()).isEqualTo(playerId.toString());
        assertThat(claims.get("username", String.class)).isEqualTo("neo");
        assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
    }

    @Test
    void rejectsExpiredToken() {
        Clock twoHoursAgo = Clock.fixed(Instant.now().minus(Duration.ofHours(2)), ZoneOffset.UTC);
        String alreadyExpired = new JwtService(properties, twoHoursAgo).issue(UUID.randomUUID(), "neo");

        assertThatThrownBy(() -> jwtService.parse(alreadyExpired))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void rejectsTamperedToken() {
        String token = jwtService.issue(UUID.randomUUID(), "neo");
        // Flip the first character of the signature segment. Unlike the segment's last character
        // (which carries don't-care base64 padding bits), the first character is always
        // bit-significant, so the decoded signature is guaranteed to differ.
        int signatureStart = token.lastIndexOf('.') + 1;
        char original = token.charAt(signatureStart);
        String tampered = token.substring(0, signatureStart)
                + (original == 'A' ? 'B' : 'A')
                + token.substring(signatureStart + 1);

        assertThatThrownBy(() -> jwtService.parse(tampered))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsTokenSignedWithAnotherSecret() {
        JwtProperties otherSecret = new JwtProperties("another-secret-another-secret-another-xx", Duration.ofHours(1));
        String foreignToken = new JwtService(otherSecret).issue(UUID.randomUUID(), "neo");

        assertThatThrownBy(() -> jwtService.parse(foreignToken))
                .isInstanceOf(JwtException.class);
    }
}
