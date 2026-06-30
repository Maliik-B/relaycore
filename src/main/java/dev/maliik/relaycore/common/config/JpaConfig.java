package dev.maliik.relaycore.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Turns on Spring Data JPA auditing so {@code @CreatedDate} / {@code @LastModifiedDate} fields are
 * populated automatically on persist and update.
 */
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}
