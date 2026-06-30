package dev.maliik.relaycore.common.outbox;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Wires the outbox infrastructure: enables scheduling (so {@link OutboxRelay} runs) and binds
 * {@link OutboxProperties}.
 */
@Configuration
@EnableScheduling
@EnableConfigurationProperties(OutboxProperties.class)
public class OutboxConfig {
}
