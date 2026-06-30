package dev.maliik.relaycore.matchmaking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.maliik.relaycore.TestcontainersConfiguration;
import dev.maliik.relaycore.common.event.MatchCompleted;
import dev.maliik.relaycore.common.event.RewardGranted;
import dev.maliik.relaycore.common.event.Topics;
import dev.maliik.relaycore.common.outbox.OutboxEvent;
import dev.maliik.relaycore.common.outbox.OutboxEventRepository;
import dev.maliik.relaycore.common.outbox.OutboxStatus;
import dev.maliik.relaycore.inventory.web.dto.InventoryView;
import dev.maliik.relaycore.inventory.web.dto.ItemView;
import dev.maliik.relaycore.matchmaking.web.dto.MatchView;
import dev.maliik.relaycore.players.web.dto.AuthResponse;

/**
 * End-to-end coverage of the M3 event loop against real Postgres + Redis + Kafka: reporting a match
 * result writes a match + a match-completed outbox row in one transaction; the relay publishes it; the
 * inventory consumer (a real {@code @KafkaListener}) grants the reward and appends a reward-granted
 * outbox row, which the relay publishes in turn. The reward-granted assertion checks the outbox row
 * reaches {@code SENT} — the relay flips that flag only after Kafka acknowledges the publish, so it is
 * proof the event was emitted. Also asserts the loop's headline property: a redelivered match event
 * applies the reward <b>exactly once</b> (event id used as the grant idempotency key). Requires Docker.
 *
 * <p>Deliberately shares the {@code @Import(TestcontainersConfiguration.class)} context with the other
 * integration tests (no extra beans) so it reuses one set of containers rather than starting its own.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class MatchRewardLoopIntegrationTest {

    private static final long REWARD_CURRENCY = 100L;
    private static final String REWARD_ITEM = "potion_health";
    private static final String REWARD_GRANTED_TYPE = RewardGranted.class.getSimpleName();

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OutboxEventRepository outbox;

    @Test
    void reportingAMatchResultGrantsTheRewardAndEmitsRewardGranted() throws Exception {
        AuthResponse auth = register("matchwinner");
        UUID winner = auth.player().id();
        String token = auth.accessToken();
        UUID matchId = UUID.randomUUID();

        ResponseEntity<MatchView> reported = rest.exchange("/matches/" + matchId + "/result", HttpMethod.POST,
                new HttpEntity<>(Map.of("winnerId", winner.toString()), bearer(token)), MatchView.class);

        assertThat(reported.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(reported.getBody().status()).isEqualTo("COMPLETED");
        assertThat(reported.getBody().winnerId()).isEqualTo(winner);

        // The consumer grants the reward asynchronously once the relay publishes match-completed.
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            InventoryView inventory = inventory(winner, token);
            assertThat(inventory.balance()).isEqualTo(REWARD_CURRENCY);
            assertThat(quantityOf(inventory, REWARD_ITEM)).isEqualTo(1);
        });

        // ...and the grant transaction's reward-granted event is emitted to Kafka via the outbox.
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            List<OutboxEvent> rewards = outbox.findByAggregateIdAndType(winner.toString(), REWARD_GRANTED_TYPE);
            assertThat(rewards).hasSize(1);
            assertThat(rewards.get(0).getStatus()).isEqualTo(OutboxStatus.SENT);
        });
        RewardGranted emitted = objectMapper.readValue(
                outbox.findByAggregateIdAndType(winner.toString(), REWARD_GRANTED_TYPE).get(0).getPayload(),
                RewardGranted.class);
        assertThat(emitted.matchId()).isEqualTo(matchId);
        assertThat(emitted.currency()).isEqualTo(REWARD_CURRENCY);
        assertThat(emitted.items()).extracting(RewardGranted.RewardItem::itemId).contains(REWARD_ITEM);
    }

    @Test
    void redeliveredMatchEventAppliesTheRewardExactlyOnce() throws Exception {
        AuthResponse auth = register("redeliverywinner");
        UUID winner = auth.player().id();
        String token = auth.accessToken();
        UUID matchId = UUID.randomUUID();

        // Same event id twice = a broker redelivery. Publish straight to the topic, bypassing the producer.
        MatchCompleted event = new MatchCompleted(UUID.randomUUID(), matchId, winner, Instant.now());
        String payload = objectMapper.writeValueAsString(event);
        kafkaTemplate.send(Topics.MATCH_COMPLETED, matchId.toString(), payload).get();
        kafkaTemplate.send(Topics.MATCH_COMPLETED, matchId.toString(), payload).get();

        // The reward lands once and stays there: balance never doubles and only one reward-granted is emitted.
        await().during(Duration.ofSeconds(3)).atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            assertThat(inventory(winner, token).balance()).isEqualTo(REWARD_CURRENCY);
            assertThat(outbox.findByAggregateIdAndType(winner.toString(), REWARD_GRANTED_TYPE)).hasSize(1);
        });
    }

    // --- helpers ---

    private AuthResponse register(String username) {
        AuthResponse body = rest.postForEntity("/auth/register",
                Map.of("username", username, "email", username + "@example.com", "password", "password123"),
                AuthResponse.class).getBody();
        assertThat(body).isNotNull();
        return body;
    }

    private InventoryView inventory(UUID playerId, String token) {
        return rest.exchange("/players/" + playerId + "/inventory", HttpMethod.GET,
                new HttpEntity<>(bearer(token)), InventoryView.class).getBody();
    }

    private int quantityOf(InventoryView view, String itemId) {
        return view.items().stream()
                .filter(item -> item.itemId().equals(itemId))
                .mapToInt(ItemView::quantity)
                .findFirst()
                .orElse(0);
    }

    private HttpHeaders bearer(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
