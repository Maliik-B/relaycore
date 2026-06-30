package dev.maliik.relaycore.players.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import dev.maliik.relaycore.players.domain.Player;

public interface PlayerRepository extends JpaRepository<Player, UUID> {

    Optional<Player> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
