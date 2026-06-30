package dev.maliik.relaycore.players.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import dev.maliik.relaycore.common.error.DuplicateResourceException;
import dev.maliik.relaycore.common.error.InvalidCredentialsException;
import dev.maliik.relaycore.common.error.ResourceNotFoundException;
import dev.maliik.relaycore.common.security.JwtService;
import dev.maliik.relaycore.players.domain.Player;
import dev.maliik.relaycore.players.repository.PlayerRepository;
import dev.maliik.relaycore.players.web.dto.PlayerProfile;

@ExtendWith(MockitoExtension.class)
class PlayerServiceTest {

    @Mock
    private PlayerRepository players;

    @Mock
    private JwtService jwtService;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private PlayerService service;

    @BeforeEach
    void setUp() {
        service = new PlayerService(players, passwordEncoder, jwtService);
    }

    @Test
    void registerHashesPasswordAndIssuesToken() {
        when(players.existsByUsername("neo")).thenReturn(false);
        when(players.existsByEmail("neo@example.com")).thenReturn(false);
        when(players.save(any(Player.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.issue(any(UUID.class), eq("neo"))).thenReturn("signed.jwt.token");
        when(jwtService.expirySeconds()).thenReturn(3600L);

        AuthResult result = service.register("neo", "neo@example.com", "password123");

        assertThat(result.token()).isEqualTo("signed.jwt.token");
        assertThat(result.expiresInSeconds()).isEqualTo(3600L);
        assertThat(result.player().username()).isEqualTo("neo");
        assertThat(result.player().displayName()).isEqualTo("neo");

        ArgumentCaptor<Player> saved = ArgumentCaptor.forClass(Player.class);
        verify(players).save(saved.capture());
        assertThat(saved.getValue().getPasswordHash()).isNotEqualTo("password123");
        assertThat(passwordEncoder.matches("password123", saved.getValue().getPasswordHash())).isTrue();
    }

    @Test
    void registerRejectsDuplicateUsername() {
        when(players.existsByUsername("neo")).thenReturn(true);

        assertThatThrownBy(() -> service.register("neo", "neo@example.com", "password123"))
                .isInstanceOf(DuplicateResourceException.class);

        verify(players, never()).save(any());
    }

    @Test
    void registerRejectsDuplicateEmail() {
        when(players.existsByUsername("neo")).thenReturn(false);
        when(players.existsByEmail("neo@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.register("neo", "neo@example.com", "password123"))
                .isInstanceOf(DuplicateResourceException.class);

        verify(players, never()).save(any());
    }

    @Test
    void loginIssuesTokenForValidCredentials() {
        Player player = Player.register("neo", "neo@example.com", passwordEncoder.encode("password123"));
        when(players.findByUsername("neo")).thenReturn(Optional.of(player));
        when(jwtService.issue(player.getId(), "neo")).thenReturn("signed.jwt.token");
        when(jwtService.expirySeconds()).thenReturn(3600L);

        AuthResult result = service.login("neo", "password123");

        assertThat(result.token()).isEqualTo("signed.jwt.token");
        assertThat(result.player().username()).isEqualTo("neo");
    }

    @Test
    void loginRejectsWrongPassword() {
        Player player = Player.register("neo", "neo@example.com", passwordEncoder.encode("password123"));
        when(players.findByUsername("neo")).thenReturn(Optional.of(player));

        assertThatThrownBy(() -> service.login("neo", "wrong-password"))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(jwtService, never()).issue(any(), any());
    }

    @Test
    void loginRejectsUnknownUser() {
        when(players.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login("ghost", "password123"))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(jwtService, never()).issue(any(), any());
    }

    @Test
    void getProfileReturnsReadModel() {
        Player player = Player.register("neo", "neo@example.com", "hash");
        when(players.findById(player.getId())).thenReturn(Optional.of(player));

        PlayerProfile profile = service.getProfile(player.getId());

        assertThat(profile.id()).isEqualTo(player.getId());
        assertThat(profile.username()).isEqualTo("neo");
        assertThat(profile.matchesPlayed()).isZero();
    }

    @Test
    void getProfileThrowsWhenMissing() {
        UUID unknown = UUID.randomUUID();
        when(players.findById(unknown)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getProfile(unknown))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateDisplayNameChangesNameAndReturnsProfile() {
        Player player = Player.register("neo", "neo@example.com", "hash");
        when(players.findById(player.getId())).thenReturn(Optional.of(player));

        PlayerProfile profile = service.updateDisplayName(player.getId(), "Trinity");

        assertThat(profile.displayName()).isEqualTo("Trinity");
        assertThat(player.getDisplayName()).isEqualTo("Trinity");
    }
}
