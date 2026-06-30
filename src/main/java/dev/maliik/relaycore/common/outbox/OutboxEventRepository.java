package dev.maliik.relaycore.common.outbox;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    /** Oldest-first batch of unpublished events for the relay to drain (backed by a partial index). */
    List<OutboxEvent> findByStatusOrderByCreatedAtAsc(OutboxStatus status, Pageable pageable);

    /** Events emitted for one aggregate of a given type — used by tests to assert the loop's output. */
    List<OutboxEvent> findByAggregateIdAndType(String aggregateId, String type);

    /**
     * Flips a published event to SENT in its own short transaction — called only after Kafka has
     * acknowledged the publish, so a crash before this runs simply leaves the row PENDING for retry.
     */
    @Transactional
    @Modifying
    @Query("update OutboxEvent o set o.status = dev.maliik.relaycore.common.outbox.OutboxStatus.SENT, "
            + "o.sentAt = :sentAt where o.id = :id")
    void markSent(@Param("id") UUID id, @Param("sentAt") Instant sentAt);
}
