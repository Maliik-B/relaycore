package dev.maliik.relaycore.common.outbox;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Polls the outbox for PENDING events and publishes them to Kafka, marking each SENT only after the
 * broker acknowledges the publish. This makes delivery <b>at-least-once</b>: a crash between a
 * successful publish and the {@code markSent} update leaves the row PENDING, so it is republished on
 * the next tick. Consumers are made idempotent (the event id is used as a grant idempotency key), so
 * at-least-once delivery becomes effectively-once processing.
 *
 * <p>A batch stops at the first publish failure and retries on the next poll, which preserves the
 * created-order of events for a given aggregate (they share a Kafka key, hence a partition).
 */
@Component
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private final OutboxEventRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final OutboxProperties properties;

    public OutboxRelay(OutboxEventRepository repository, KafkaTemplate<String, String> kafkaTemplate,
            OutboxProperties properties) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${relaycore.outbox.poll-interval-ms:1000}")
    public void publishPending() {
        List<OutboxEvent> batch = repository.findByStatusOrderByCreatedAtAsc(
                OutboxStatus.PENDING, PageRequest.of(0, properties.batchSize()));
        for (OutboxEvent event : batch) {
            if (!publish(event)) {
                break;
            }
        }
    }

    private boolean publish(OutboxEvent event) {
        try {
            kafkaTemplate.send(event.getTopic(), event.getAggregateId(), event.getPayload())
                    .get(properties.sendTimeoutMs(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted publishing outbox event {} to {}; will retry", event.getId(), event.getTopic());
            return false;
        } catch (Exception ex) {
            log.warn("Failed publishing outbox event {} to {}; will retry next poll",
                    event.getId(), event.getTopic(), ex);
            return false;
        }
        repository.markSent(event.getId(), Instant.now());
        return true;
    }
}
