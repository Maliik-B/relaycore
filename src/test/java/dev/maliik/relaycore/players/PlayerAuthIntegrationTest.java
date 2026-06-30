package dev.maliik.relaycore.players;

import static org.assertj.core.api.Assertions.assertThat;

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

import dev.maliik.relaycore.TestcontainersConfiguration;
import dev.maliik.relaycore.players.web.dto.AuthResponse;
import dev.maliik.relaycore.players.web.dto.PlayerProfile;

/**
 * End-to-end coverage of the players/auth slice against real Postgres + Redis (via Testcontainers):
 * register -> token -> authenticated profile read, plus the auth failure modes and cache-evicting
 * profile update. Requires a running Docker daemon.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class PlayerAuthIntegrationTest {

    @Autowired
    private TestRestTemplate rest;

    @Test
    void registerThenReadOwnProfileWithToken() {
        AuthResponse auth = register("neo", "neo@example.com", "password123").getBody();
        assertThat(auth).isNotNull();
        assertThat(auth.accessToken()).isNotBlank();
        assertThat(auth.tokenType()).isEqualTo("Bearer");
        assertThat(auth.player().username()).isEqualTo("neo");

        ResponseEntity<PlayerProfile> me = rest.exchange("/players/me", HttpMethod.GET,
                new HttpEntity<>(bearer(auth.accessToken())), PlayerProfile.class);

        assertThat(me.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(me.getBody()).isNotNull();
        assertThat(me.getBody().username()).isEqualTo("neo");
        assertThat(me.getBody().matchesPlayed()).isZero();
    }

    @Test
    void profileEndpointRequiresAuthentication() {
        ResponseEntity<String> me = rest.getForEntity("/players/me", String.class);

        assertThat(me.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void loginReturnsTokenAndRejectsBadPassword() {
        register("trinity", "trinity@example.com", "password123");

        ResponseEntity<AuthResponse> ok = rest.postForEntity("/auth/login",
                Map.of("username", "trinity", "password", "password123"), AuthResponse.class);
        assertThat(ok.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(ok.getBody()).isNotNull();
        assertThat(ok.getBody().accessToken()).isNotBlank();

        ResponseEntity<String> bad = rest.postForEntity("/auth/login",
                Map.of("username", "trinity", "password", "wrong-password"), String.class);
        assertThat(bad.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void duplicateRegistrationIsRejected() {
        register("morpheus", "morpheus@example.com", "password123");

        ResponseEntity<String> conflict = rest.postForEntity("/auth/register",
                Map.of("username", "morpheus", "email", "other@example.com", "password", "password123"),
                String.class);

        assertThat(conflict.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void invalidRegistrationPayloadIsRejected() {
        ResponseEntity<String> response = rest.postForEntity("/auth/register",
                Map.of("username", "x", "email", "not-an-email", "password", "short"),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void readsAnotherPlayerById() {
        AuthResponse reader = register("oracle", "oracle@example.com", "password123").getBody();
        AuthResponse target = register("cypher", "cypher@example.com", "password123").getBody();
        assertThat(reader).isNotNull();
        assertThat(target).isNotNull();
        UUID targetId = target.player().id();

        ResponseEntity<PlayerProfile> response = rest.exchange("/players/" + targetId, HttpMethod.GET,
                new HttpEntity<>(bearer(reader.accessToken())), PlayerProfile.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().username()).isEqualTo("cypher");
    }

    @Test
    void unknownPlayerIdReturnsNotFound() {
        AuthResponse auth = register("tank", "tank@example.com", "password123").getBody();
        assertThat(auth).isNotNull();

        ResponseEntity<String> response = rest.exchange("/players/" + UUID.randomUUID(), HttpMethod.GET,
                new HttpEntity<>(bearer(auth.accessToken())), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void updatingDisplayNameIsReflectedOnNextRead() {
        AuthResponse auth = register("dozer", "dozer@example.com", "password123").getBody();
        assertThat(auth).isNotNull();
        HttpHeaders headers = bearer(auth.accessToken());

        // Prime the profile cache.
        rest.exchange("/players/me", HttpMethod.GET, new HttpEntity<>(headers), PlayerProfile.class);

        ResponseEntity<PlayerProfile> updated = rest.exchange("/players/me", HttpMethod.PATCH,
                new HttpEntity<>(Map.of("displayName", "Dozer the Operator"), headers), PlayerProfile.class);
        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updated.getBody()).isNotNull();
        assertThat(updated.getBody().displayName()).isEqualTo("Dozer the Operator");

        // The cache should have been evicted by the update, so the next read returns the new name.
        ResponseEntity<PlayerProfile> reread = rest.exchange("/players/me", HttpMethod.GET,
                new HttpEntity<>(headers), PlayerProfile.class);
        assertThat(reread.getBody()).isNotNull();
        assertThat(reread.getBody().displayName()).isEqualTo("Dozer the Operator");
    }

    private ResponseEntity<AuthResponse> register(String username, String email, String password) {
        return rest.postForEntity("/auth/register",
                Map.of("username", username, "email", email, "password", password), AuthResponse.class);
    }

    private HttpHeaders bearer(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
