package dev.maliik.relaycore.common.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * Enables Spring's cache abstraction application-wide. The Redis-backed cache manager is
 * auto-configured (spring.cache.type=redis); individual modules contribute per-cache settings
 * (TTL, serializer) via {@code RedisCacheManagerBuilderCustomizer} beans.
 */
@Configuration
@EnableCaching
public class CacheConfig {
}
