package dev.maliik.relaycore.common.outbox;

/**
 * Lifecycle of an {@link OutboxEvent}: {@code PENDING} until the relay confirms it reached Kafka,
 * then {@code SENT}. Stored as a string so the schema stays legible under {@code ddl-auto: validate}.
 */
public enum OutboxStatus {
    PENDING,
    SENT
}
