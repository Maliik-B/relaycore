package dev.maliik.relaycore.common.outbox;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Tuning for the outbox relay. {@code pollIntervalMs} is referenced directly in {@link OutboxRelay}'s
 * {@code @Scheduled} placeholder; {@code batchSize} caps rows drained per tick and {@code sendTimeoutMs}
 * bounds how long the relay waits for a Kafka publish ack before leaving a row PENDING for retry.
 */
@ConfigurationProperties(prefix = "relaycore.outbox")
public record OutboxProperties(
        @DefaultValue("1000") long pollIntervalMs,
        @DefaultValue("100") int batchSize,
        @DefaultValue("5000") long sendTimeoutMs) {
}
