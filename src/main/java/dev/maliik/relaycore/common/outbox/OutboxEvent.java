package dev.maliik.relaycore.common.outbox;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Transactional-outbox row: a domain event captured in the same database transaction as the state
 * change that produced it. A relay ({@link OutboxRelay}) later publishes {@code PENDING} rows to Kafka
 * and flips them to {@code SENT}. This is how the app avoids the dual-write problem — a domain write
 * and a Kafka publish can't share one atomic transaction, so the publish is deferred to a row that
 * <i>can</i> be written atomically with the domain change.
 *
 * <p>Write-once apart from the relay setting {@code status}/{@code sent_at}; the payload is an
 * already-serialized JSON string (see {@link OutboxAppender}).
 */
@Entity
@Table(name = "outbox_events")
@EntityListeners(AuditingEntityListener.class)
public class OutboxEvent {

    @Id
    private UUID id;

    @Column(name = "aggregate_type", nullable = false, length = 64)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false, length = 64)
    private String aggregateId;

    @Column(nullable = false, length = 64)
    private String type;

    @Column(nullable = false, length = 128)
    private String topic;

    @Column(nullable = false, length = 8000)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private OutboxStatus status;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    protected OutboxEvent() {
    }

    /**
     * Creates a PENDING event. {@code aggregateId} doubles as the Kafka message key so all events for
     * one aggregate land on the same partition and stay ordered.
     */
    public static OutboxEvent pending(String aggregateType, String aggregateId, String type, String topic,
            String payload) {
        OutboxEvent event = new OutboxEvent();
        event.id = UUID.randomUUID();
        event.aggregateType = aggregateType;
        event.aggregateId = aggregateId;
        event.type = type;
        event.topic = topic;
        event.payload = payload;
        event.status = OutboxStatus.PENDING;
        return event;
    }

    public void markSent(Instant when) {
        this.status = OutboxStatus.SENT;
        this.sentAt = when;
    }

    public UUID getId() {
        return id;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public String getType() {
        return type;
    }

    public String getTopic() {
        return topic;
    }

    public String getPayload() {
        return payload;
    }

    public OutboxStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getSentAt() {
        return sentAt;
    }
}
