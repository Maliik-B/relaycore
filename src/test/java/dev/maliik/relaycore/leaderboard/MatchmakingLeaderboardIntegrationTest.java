package dev.maliik.relaycore.leaderboard;

import static java.util.Comparator.comparingLong;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;

import dev.maliik.relaycore.TestcontainersConfiguration;
import dev.maliik.relaycore.common.event.MatchCompleted;
import dev.maliik.relaycore.common.event.Topics;
import dev.maliik.relaycore.common.outbox.OutboxEventRepository;
import dev.maliik.relaycore.inventory.web.dto.InventoryView;
import dev.maliik.relaycore.leaderboard.service.LeaderboardService;
import dev.maliik.relaycore.leaderboard.web.dto.LeaderboardEntry;
import dev.maliik.relaycore.leaderboard.web.dto.PlayerRankView;
import dev.maliik.relaycore.matchmaking.web.dto.MatchView;
import dev.maliik.relaycore.matchmaking.web.dto.TicketView;
import dev.maliik.relaycore.players.web.dto.AuthResponse;

/**
 * The M4 capstone: the full loop, exercised across <b>both</b> fan-out consumers. Two players queue, the
 * worker forms the match, the result is reported, and the single {@code match-completed} event drives two
 * independent consumer groups — inventory grants the winner's reward and the leaderboard (its own group)
 * ranks the winner. Then the same event is redelivered and both effects hold: the reward is granted once
 * and the leaderboard score increments once (ZINCRBY guarded by an event-id set). Requires Docker.
 *
 * <p>Shares the {@code @Import(TestcontainersConfiguration.class)} context (no extra beans) with the other
 * integration tests so they reuse one container set. Each run uses a unique region so the worker pairs
 * exactly this test's two players.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class MatchmakingLeaderboardIntegrationTest {

    private static final long REWARD_CURRENCY = 100L;
    private static final String SEASON = LeaderboardService.DEFAULT_SEASON;
    private static final String MATCH_COMPLETED_TYPE = MatchCompleted.class.getSimpleName();

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private OutboxEventRepository outbox;

    @Test
    void completedMatchGrantsRewardAndRanksWinnerAcrossBothConsumers() throws Exception {
        String region = "cap-" + UUID.randomUUID().toString().substring(0, 8);
        AuthResponse winnerAuth = register("capwinner");
        AuthResponse opponentAuth = register("caploser");
        UUID winner = winnerAuth.player().id();
        String token = winnerAuth.accessToken();

        UUID winnerTicket = queue(token, region, 1).id();
        queue(opponentAuth.accessToken(), region, 1);
        UUID matchId = await().atMost(Duration.ofSeconds(30))
                .until(() -> ticket(winnerTicket, token).matchId(), Objects::nonNull);

        MatchView reported = rest.exchange("/matches/" + matchId + "/result", HttpMethod.POST,
                new HttpEntity<>(Map.of("winnerId", winner.toString()), bearer(token)), MatchView.class).getBody();
        assertThat(reported.status()).isEqualTo("COMPLETED");
        assertThat(reported.participantIds()).contains(winner, opponentAuth.player().id());

        // Inventory consumer (group "relaycore") grants the reward.
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(inventory(winner, token).balance()).isEqualTo(REWARD_CURRENCY));

        // Leaderboard consumer (group "relaycore-leaderboard") ranks the winner — independent fan-out.
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            ResponseEntity<PlayerRankView> rank = rankResponse(winner, token);
            assertThat(rank.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(rank.getBody().score()).isEqualTo(1L);
            assertThat(rank.getBody().rank()).isGreaterThanOrEqualTo(1L);
        });

        List<LeaderboardEntry> top = top(100, token);
        assertThat(top).isNotEmpty();
        assertThat(top).isSortedAccordingTo(comparingLong(LeaderboardEntry::score).reversed());

        // Redeliver the exact emitted event twice; neither effect double-applies.
        String payload = outbox.findByAggregateIdAndType(matchId.toString(), MATCH_COMPLETED_TYPE).get(0).getPayload();
        kafkaTemplate.send(Topics.MATCH_COMPLETED, matchId.toString(), payload).get();
        kafkaTemplate.send(Topics.MATCH_COMPLETED, matchId.toString(), payload).get();

        await().during(Duration.ofSeconds(3)).atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            assertThat(rankResponse(winner, token).getBody().score()).isEqualTo(1L);
            assertThat(inventory(winner, token).balance()).isEqualTo(REWARD_CURRENCY);
        });
    }

    // --- helpers ---

    private AuthResponse register(String username) {
        String unique = username + "_" + UUID.randomUUID().toString().substring(0, 8);
        AuthResponse body = rest.postForEntity("/auth/register",
                Map.of("username", unique, "email", unique + "@example.com", "password", "password123"),
                AuthResponse.class).getBody();
        assertThat(body).isNotNull();
        return body;
    }

    private TicketView queue(String token, String region, int skillBucket) {
        return rest.exchange("/matchmaking/queue", HttpMethod.POST,
                new HttpEntity<>(Map.of("region", region, "skillBucket", skillBucket), bearer(token)),
                TicketView.class).getBody();
    }

    private TicketView ticket(UUID ticketId, String token) {
        return rest.exchange("/matchmaking/tickets/" + ticketId, HttpMethod.GET,
                new HttpEntity<>(bearer(token)), TicketView.class).getBody();
    }

    private InventoryView inventory(UUID playerId, String token) {
        return rest.exchange("/players/" + playerId + "/inventory", HttpMethod.GET,
                new HttpEntity<>(bearer(token)), InventoryView.class).getBody();
    }

    private ResponseEntity<PlayerRankView> rankResponse(UUID playerId, String token) {
        return rest.exchange("/leaderboards/" + SEASON + "/players/" + playerId + "/rank", HttpMethod.GET,
                new HttpEntity<>(bearer(token)), PlayerRankView.class);
    }

    private List<LeaderboardEntry> top(int n, String token) {
        return rest.exchange("/leaderboards/" + SEASON + "/top?n=" + n, HttpMethod.GET,
                new HttpEntity<>(bearer(token)), new ParameterizedTypeReference<List<LeaderboardEntry>>() {
                }).getBody();
    }

    private HttpHeaders bearer(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
