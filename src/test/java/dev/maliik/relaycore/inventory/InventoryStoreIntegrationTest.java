package dev.maliik.relaycore.inventory;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

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

import dev.maliik.relaycore.TestcontainersConfiguration;
import dev.maliik.relaycore.inventory.web.dto.CatalogItemView;
import dev.maliik.relaycore.inventory.web.dto.InventoryView;
import dev.maliik.relaycore.inventory.web.dto.ItemView;
import dev.maliik.relaycore.inventory.web.dto.PurchaseResult;
import dev.maliik.relaycore.players.web.dto.AuthResponse;

/**
 * End-to-end coverage of inventory/store against real Postgres + Redis: catalog, idempotent grants
 * (including replay and key-reuse conflict), purchases and their failure modes, and the two
 * correctness showcases — optimistic-lock double-spend prevention and idempotent grants under
 * concurrency. Requires Docker.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class InventoryStoreIntegrationTest {

    @Autowired
    private TestRestTemplate rest;

    @Test
    void catalogListsEnabledItemsCheapestFirst() {
        String token = register("catalogviewer").accessToken();

        ResponseEntity<CatalogItemView[]> response = rest.exchange("/store/catalog", HttpMethod.GET,
                new HttpEntity<>(bearer(token)), CatalogItemView[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<CatalogItemView> catalog = List.of(response.getBody());
        assertThat(catalog).extracting(CatalogItemView::itemId).doesNotContain("legacy_banner");
        assertThat(catalog).isSortedAccordingTo((a, b) -> Long.compare(a.price(), b.price()));
        assertThat(catalog.get(0).itemId()).isEqualTo("potion_health");
    }

    @Test
    void grantAddsCurrencyAndItemsThenInventoryReflectsThem() {
        AuthResponse auth = register("granter");
        UUID id = auth.player().id();

        ResponseEntity<InventoryView> granted = grant(id, auth.accessToken(), "grant-1",
                Map.of("currency", 200, "items", List.of(Map.of("itemId", "sword_iron", "quantity", 1))));

        assertThat(granted.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(granted.getHeaders().getFirst("Idempotency-Replayed")).isEqualTo("false");
        assertThat(granted.getBody().balance()).isEqualTo(200);
        assertThat(quantityOf(granted.getBody(), "sword_iron")).isEqualTo(1);

        ResponseEntity<InventoryView> fetched = rest.exchange("/players/" + id + "/inventory", HttpMethod.GET,
                new HttpEntity<>(bearer(auth.accessToken())), InventoryView.class);
        assertThat(fetched.getBody().balance()).isEqualTo(200);
        assertThat(quantityOf(fetched.getBody(), "sword_iron")).isEqualTo(1);
    }

    @Test
    void replayingAGrantWithTheSameKeyDoesNotDoubleApply() {
        AuthResponse auth = register("replayer");
        UUID id = auth.player().id();
        Map<String, Object> body = Map.of("currency", 150);

        ResponseEntity<InventoryView> first = grant(id, auth.accessToken(), "grant-replay", body);
        ResponseEntity<InventoryView> second = grant(id, auth.accessToken(), "grant-replay", body);

        assertThat(first.getHeaders().getFirst("Idempotency-Replayed")).isEqualTo("false");
        assertThat(second.getHeaders().getFirst("Idempotency-Replayed")).isEqualTo("true");
        assertThat(second.getBody().balance()).isEqualTo(150);
        assertThat(currentBalance(id, auth.accessToken())).isEqualTo(150);
    }

    @Test
    void reusingAnIdempotencyKeyForADifferentRequestConflicts() {
        AuthResponse auth = register("keyreuser");
        UUID id = auth.player().id();

        grant(id, auth.accessToken(), "grant-reuse", Map.of("currency", 100));
        ResponseEntity<String> conflict = rest.exchange("/players/" + id + "/inventory/grant", HttpMethod.POST,
                new HttpEntity<>(Map.of("currency", 999), withKey(bearer(auth.accessToken()), "grant-reuse")),
                String.class);

        assertThat(conflict.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(currentBalance(id, auth.accessToken())).isEqualTo(100);
    }

    @Test
    void grantWithoutIdempotencyKeyIsRejected() {
        AuthResponse auth = register("nokey");
        UUID id = auth.player().id();

        ResponseEntity<String> response = rest.exchange("/players/" + id + "/inventory/grant", HttpMethod.POST,
                new HttpEntity<>(Map.of("currency", 100), bearer(auth.accessToken())), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void purchaseDebitsCurrencyAndGrantsItem() {
        AuthResponse auth = register("buyer");
        UUID id = auth.player().id();
        grant(id, auth.accessToken(), "fund-buyer", Map.of("currency", 1000));

        ResponseEntity<PurchaseResult> purchase = rest.exchange("/store/purchase", HttpMethod.POST,
                new HttpEntity<>(Map.of("itemId", "sword_iron", "quantity", 2), bearer(auth.accessToken())),
                PurchaseResult.class);

        assertThat(purchase.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(purchase.getBody().balance()).isEqualTo(800);
        assertThat(purchase.getBody().purchased().quantity()).isEqualTo(2);
        assertThat(currentBalance(id, auth.accessToken())).isEqualTo(800);
    }

    @Test
    void purchaseWithoutFundsReturns422() {
        AuthResponse auth = register("brokebuyer");

        ResponseEntity<String> response = rest.exchange("/store/purchase", HttpMethod.POST,
                new HttpEntity<>(Map.of("itemId", "sword_iron", "quantity", 1), bearer(auth.accessToken())),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void purchaseOfDisabledItemReturns409AndUnknownItemReturns404() {
        AuthResponse auth = register("edgebuyer");
        grant(auth.player().id(), auth.accessToken(), "fund-edge", Map.of("currency", 5000));

        ResponseEntity<String> disabled = rest.exchange("/store/purchase", HttpMethod.POST,
                new HttpEntity<>(Map.of("itemId", "legacy_banner", "quantity", 1), bearer(auth.accessToken())),
                String.class);
        ResponseEntity<String> unknown = rest.exchange("/store/purchase", HttpMethod.POST,
                new HttpEntity<>(Map.of("itemId", "does_not_exist", "quantity", 1), bearer(auth.accessToken())),
                String.class);

        assertThat(disabled.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(unknown.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void concurrentPurchasesNeverDoubleSpend() throws Exception {
        AuthResponse auth = register("concurrentbuyer");
        UUID id = auth.player().id();
        String token = auth.accessToken();
        grant(id, token, "fund-concurrent", Map.of("currency", 500)); // affords exactly 5 at 100 each

        int attempts = 10;
        List<Integer> statuses = runConcurrently(attempts, () -> rest.exchange("/store/purchase", HttpMethod.POST,
                new HttpEntity<>(Map.of("itemId", "sword_iron", "quantity", 1), bearer(token)), String.class)
                .getStatusCode().value());

        long successes = statuses.stream().filter(s -> s == 200).count();
        assertThat(successes).isBetween(1L, 5L);
        assertThat(statuses).filteredOn(s -> s != 200).allMatch(s -> s == 409 || s == 422);

        long balance = currentBalance(id, token);
        assertThat(balance).isEqualTo(500 - 100 * successes);
        assertThat(balance).isGreaterThanOrEqualTo(0);
        assertThat(quantityOf(inventory(id, token), "sword_iron")).isEqualTo((int) successes);
    }

    @Test
    void concurrentGrantsWithTheSameKeyApplyExactlyOnce() throws Exception {
        AuthResponse auth = register("concurrentgranter");
        UUID id = auth.player().id();
        String token = auth.accessToken();

        int attempts = 8;
        List<Integer> statuses = runConcurrently(attempts, () -> rest.exchange(
                "/players/" + id + "/inventory/grant", HttpMethod.POST,
                new HttpEntity<>(Map.of("currency", 100), withKey(bearer(token), "grant-race")), String.class)
                .getStatusCode().value());

        assertThat(statuses).allMatch(s -> s == 200);
        assertThat(currentBalance(id, token)).isEqualTo(100);
    }

    // --- helpers ---

    private List<Integer> runConcurrently(int count, java.util.concurrent.Callable<Integer> task) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(count);
        CountDownLatch ready = new CountDownLatch(1);
        List<Future<Integer>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < count; i++) {
                futures.add(pool.submit(() -> {
                    ready.await();
                    return task.call();
                }));
            }
            ready.countDown();
            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> future : futures) {
                statuses.add(future.get(30, TimeUnit.SECONDS));
            }
            return statuses;
        } finally {
            pool.shutdownNow();
        }
    }

    private AuthResponse register(String username) {
        AuthResponse body = rest.postForEntity("/auth/register",
                Map.of("username", username, "email", username + "@example.com", "password", "password123"),
                AuthResponse.class).getBody();
        assertThat(body).isNotNull();
        return body;
    }

    private ResponseEntity<InventoryView> grant(UUID playerId, String token, String key, Map<String, Object> body) {
        return rest.exchange("/players/" + playerId + "/inventory/grant", HttpMethod.POST,
                new HttpEntity<>(body, withKey(bearer(token), key)), InventoryView.class);
    }

    private InventoryView inventory(UUID playerId, String token) {
        return rest.exchange("/players/" + playerId + "/inventory", HttpMethod.GET,
                new HttpEntity<>(bearer(token)), InventoryView.class).getBody();
    }

    private long currentBalance(UUID playerId, String token) {
        return inventory(playerId, token).balance();
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

    private HttpHeaders withKey(HttpHeaders headers, String key) {
        headers.set("Idempotency-Key", key);
        return headers;
    }
}
