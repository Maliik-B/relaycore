package dev.maliik.relaycore.players.config;

import java.time.Duration;

import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.maliik.relaycore.players.web.dto.PlayerProfile;

/**
 * Cache-aside settings for player profile reads: a 10-minute TTL with string keys and JSON-serialized
 * {@link PlayerProfile} values. A typed JSON serializer (over Boot's Jackson mapper, which already
 * handles {@code Instant}) keeps the stored payload clean and reversible without polymorphic type
 * hints. Writes invalidate the entry — see {@code PlayerService}.
 */
@Configuration
public class PlayerCacheConfig {

    public static final String PLAYER_PROFILES_CACHE = "playerProfiles";

    @Bean
    RedisCacheManagerBuilderCustomizer playerProfileCacheCustomizer(ObjectMapper objectMapper) {
        RedisCacheConfiguration profileCache = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .disableCachingNullValues()
                .serializeKeysWith(SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(SerializationPair.fromSerializer(
                        new Jackson2JsonRedisSerializer<>(objectMapper, PlayerProfile.class)));
        return builder -> builder.withCacheConfiguration(PLAYER_PROFILES_CACHE, profileCache);
    }
}
