package dev.maliik.relaycore.matchmaking.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import dev.maliik.relaycore.matchmaking.domain.Match;

public interface MatchRepository extends JpaRepository<Match, UUID> {
}
