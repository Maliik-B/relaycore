package dev.maliik.relaycore.players.service;

import java.util.UUID;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.maliik.relaycore.common.error.DuplicateResourceException;
import dev.maliik.relaycore.common.error.InvalidCredentialsException;
import dev.maliik.relaycore.common.error.ResourceNotFoundException;
import dev.maliik.relaycore.common.security.JwtService;
import dev.maliik.relaycore.players.config.PlayerCacheConfig;
import dev.maliik.relaycore.players.domain.Player;
import dev.maliik.relaycore.players.repository.PlayerRepository;
import dev.maliik.relaycore.players.web.dto.PlayerProfile;

/**
 * Application service for the players module: registration, authentication, and cached profile reads.
 *
 * <p>Profile reads use cache-aside via Spring's cache abstraction ({@link Cacheable}): a hit skips
 * the database; a miss loads and populates the cache. Writes that change a profile evict the entry
 * ({@link CacheEvict}) so the next read repopulates it — keeping the cache from serving stale data.
 */
@Service
public class PlayerService {

    private final PlayerRepository players;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public PlayerService(PlayerRepository players, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.players = players;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResult register(String username, String email, String rawPassword) {
        if (players.existsByUsername(username)) {
            throw new DuplicateResourceException("username", username);
        }
        if (players.existsByEmail(email)) {
            throw new DuplicateResourceException("email", email);
        }
        Player player = players.save(Player.register(username, email, passwordEncoder.encode(rawPassword)));
        return issueFor(player);
    }

    @Transactional(readOnly = true)
    public AuthResult login(String username, String rawPassword) {
        Player player = players.findByUsername(username)
                .filter(p -> passwordEncoder.matches(rawPassword, p.getPasswordHash()))
                .orElseThrow(InvalidCredentialsException::new);
        return issueFor(player);
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = PlayerCacheConfig.PLAYER_PROFILES_CACHE, key = "#id")
    public PlayerProfile getProfile(UUID id) {
        return players.findById(id)
                .map(PlayerProfile::from)
                .orElseThrow(() -> new ResourceNotFoundException("player", id.toString()));
    }

    @Transactional
    @CacheEvict(cacheNames = PlayerCacheConfig.PLAYER_PROFILES_CACHE, key = "#id")
    public PlayerProfile updateDisplayName(UUID id, String displayName) {
        Player player = players.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("player", id.toString()));
        player.setDisplayName(displayName);
        return PlayerProfile.from(player);
    }

    private AuthResult issueFor(Player player) {
        String token = jwtService.issue(player.getId(), player.getUsername());
        return new AuthResult(token, jwtService.expirySeconds(), PlayerProfile.from(player));
    }
}
