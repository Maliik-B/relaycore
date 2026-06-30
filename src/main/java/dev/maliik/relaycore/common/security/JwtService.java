package dev.maliik.relaycore.common.security;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Issues and verifies the HMAC-signed JWTs used for stateless auth. The token subject is the player
 * id; the username is carried as a claim so the filter can build a principal without a DB round-trip.
 *
 * <p>The {@link Clock} is injectable so token-expiry behaviour is deterministically testable.
 */
@Service
public class JwtService {

    private static final String CLAIM_USERNAME = "username";

    private final SecretKey key;
    private final long expirySeconds;
    private final Clock clock;

    @Autowired
    public JwtService(JwtProperties properties) {
        this(properties, Clock.systemUTC());
    }

    JwtService(JwtProperties properties, Clock clock) {
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
        this.expirySeconds = properties.expiry().toSeconds();
        this.clock = clock;
    }

    /** Issues a signed token for the given player. */
    public String issue(UUID playerId, String username) {
        Instant now = clock.instant();
        return Jwts.builder()
                .subject(playerId.toString())
                .claim(CLAIM_USERNAME, username)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expirySeconds)))
                .signWith(key)
                .compact();
    }

    /**
     * Verifies the signature and expiry of a token and returns its parsed claims.
     *
     * @throws io.jsonwebtoken.JwtException if the token is malformed, tampered with, or expired.
     */
    public Jws<Claims> parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token);
    }

    /** Token lifetime in seconds — surfaced to clients in the auth response. */
    public long expirySeconds() {
        return expirySeconds;
    }
}
