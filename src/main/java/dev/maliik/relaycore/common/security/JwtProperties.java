package dev.maliik.relaycore.common.security;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Externalised JWT signing configuration, bound from {@code relaycore.jwt.*}.
 *
 * @param secret HMAC signing secret; must be at least 32 bytes for HS256. Supplied via env in prod.
 * @param expiry how long an issued access token remains valid.
 */
@ConfigurationProperties(prefix = "relaycore.jwt")
public record JwtProperties(String secret, Duration expiry) {
}
