package dev.maliik.relaycore.matchmaking.domain;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A match. In M3 a match is created directly in the {@code COMPLETED} state when its result is
 * reported — matchmaking exists here only to be the event <i>source</i> for the reward loop. The full
 * FORMING → ACTIVE → COMPLETED lifecycle (queue + worker) arrives in M4. {@code winner_id} is an
 * opaque reference into the players module — no cross-module FK.
 */
@Entity
@Table(name = "matches")
@EntityListeners(AuditingEntityListener.class)
public class Match {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private MatchStatus status;

    @Column(name = "winner_id", nullable = false)
    private UUID winnerId;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected Match() {
    }

    public static Match completed(UUID id, UUID winnerId, Instant completedAt) {
        Match match = new Match();
        match.id = id;
        match.winnerId = winnerId;
        match.status = MatchStatus.COMPLETED;
        match.completedAt = completedAt;
        return match;
    }

    public UUID getId() {
        return id;
    }

    public MatchStatus getStatus() {
        return status;
    }

    public UUID getWinnerId() {
        return winnerId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }
}
