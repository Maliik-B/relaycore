package dev.maliik.relaycore.common.outbox;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Domain-facing entry point for emitting an event: serialize the payload to JSON and persist a PENDING
 * {@link OutboxEvent}. Call this <b>inside the domain transaction</b> — the row commits atomically with
 * the state change, and the relay handles the actual Kafka publish afterwards.
 *
 * <p>Payloads are stored as a JSON string (and published as a string value) so the consumer side needs
 * no {@code JsonDeserializer} trusted-package configuration — it parses with the same {@link ObjectMapper}.
 */
@Component
public class OutboxAppender {

    private final OutboxEventRepository repository;
    private final ObjectMapper objectMapper;

    public OutboxAppender(OutboxEventRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public void append(String aggregateType, String aggregateId, String type, String topic, Object payload) {
        repository.save(OutboxEvent.pending(aggregateType, aggregateId, type, topic, serialize(payload)));
    }

    private String serialize(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize outbox payload of type "
                    + payload.getClass().getName(), ex);
        }
    }
}
