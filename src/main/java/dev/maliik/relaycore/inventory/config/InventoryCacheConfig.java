package dev.maliik.relaycore.inventory.config;

import java.time.Duration;

import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.maliik.relaycore.inventory.web.dto.InventoryView;

/**
 * Cache-aside settings for inventory reads: a 10-minute TTL with string keys and JSON-serialized
 * {@link InventoryView} values. Grants and purchases evict the player's entry so a stale balance or
 * item count is never served.
 */
@Configuration
public class InventoryCacheConfig {

    public static final String INVENTORY_CACHE = "playerInventory";

    @Bean
    RedisCacheManagerBuilderCustomizer inventoryCacheCustomizer(ObjectMapper objectMapper) {
        RedisCacheConfiguration inventoryCache = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .disableCachingNullValues()
                .serializeKeysWith(SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(SerializationPair.fromSerializer(
                        new Jackson2JsonRedisSerializer<>(objectMapper, InventoryView.class)));
        return builder -> builder.withCacheConfiguration(INVENTORY_CACHE, inventoryCache);
    }
}
