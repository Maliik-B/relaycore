package dev.maliik.relaycore.inventory.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.maliik.relaycore.common.event.MatchCompleted;
import dev.maliik.relaycore.common.event.Topics;
import dev.maliik.relaycore.inventory.service.InventoryService;

/**
 * Consumes {@link Topics#MATCH_COMPLETED} and grants the match reward. The payload is a JSON string
 * (matching how the outbox publishes), parsed with the shared {@link ObjectMapper} — no
 * {@code JsonDeserializer} trusted-package setup needed. Idempotency lives in
 * {@link InventoryService#applyMatchReward} (keyed on the event id), so a redelivery is harmless and
 * this listener can stay a thin adapter.
 */
@Component
public class MatchCompletedListener {

    private static final Logger log = LoggerFactory.getLogger(MatchCompletedListener.class);

    private final InventoryService inventoryService;
    private final ObjectMapper objectMapper;

    public MatchCompletedListener(InventoryService inventoryService, ObjectMapper objectMapper) {
        this.inventoryService = inventoryService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = Topics.MATCH_COMPLETED)
    public void onMatchCompleted(String payload) {
        MatchCompleted event = parse(payload);
        log.info("Applying match reward to player {} for match {} (event {})",
                event.winnerId(), event.matchId(), event.eventId());
        inventoryService.applyMatchReward(event);
    }

    private MatchCompleted parse(String payload) {
        try {
            return objectMapper.readValue(payload, MatchCompleted.class);
        } catch (Exception ex) {
            throw new IllegalStateException("Unparseable match-completed payload: " + payload, ex);
        }
    }
}
