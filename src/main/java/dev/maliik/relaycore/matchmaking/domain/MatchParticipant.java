package dev.maliik.relaycore.matchmaking.domain;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * One player's membership in a match. Modelled as a standalone entity (keyed by id, referencing the match
 * by {@code matchId}) rather than a JPA {@code @OneToMany}/{@code @ElementCollection} — this keeps the
 * mapping validate-friendly and matches how the rest of the codebase references across boundaries by id.
 * {@code playerId} is opaque (no cross-module FK); {@code matchId} is FK'd to {@code matches}.
 */
@Entity
@Table(name = "match_participants")
@EntityListeners(AuditingEntityListener.class)
public class MatchParticipant {

    @Id
    private UUID id;

    @Column(name = "match_id", nullable = false)
    private UUID matchId;

    @Column(name = "player_id", nullable = false)
    private UUID playerId;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected MatchParticipant() {
    }

    public static MatchParticipant of(UUID matchId, UUID playerId) {
        MatchParticipant participant = new MatchParticipant();
        participant.id = UUID.randomUUID();
        participant.matchId = matchId;
        participant.playerId = playerId;
        return participant;
    }

    public UUID getId() {
        return id;
    }

    public UUID getMatchId() {
        return matchId;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
