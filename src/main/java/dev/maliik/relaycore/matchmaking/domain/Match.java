package dev.maliik.relaycore.matchmaking.domain;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import dev.maliik.relaycore.common.error.MatchAlreadyCompletedException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * A match formed by the matchmaking worker from a bucket of queued tickets, and the event <i>source</i>
 * of the reward loop. A match is created {@code ACTIVE} (winner unknown); reporting a result transitions
 * it to {@code COMPLETED} and records the winner. The {@code @Version} column makes that transition
 * optimistically locked, so a concurrent double-report loses on commit rather than emitting the reward
 * event twice. {@code winnerId} is an opaque reference into the players module — no cross-module FK.
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

    @Column(nullable = false, length = 32)
    private String region;

    @Column(name = "skill_bucket", nullable = false)
    private int skillBucket;

    @Column(name = "winner_id")
    private UUID winnerId;

    @Version
    @Column(nullable = false)
    private long version;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected Match() {
    }

    /** Creates an ACTIVE match for a formed bucket; the id is assigned here so participants can reference it. */
    public static Match active(String region, int skillBucket) {
        Match match = new Match();
        match.id = UUID.randomUUID();
        match.region = region;
        match.skillBucket = skillBucket;
        match.status = MatchStatus.ACTIVE;
        return match;
    }

    /** Transitions an ACTIVE match to COMPLETED, recording the winner. Rejects a non-ACTIVE match. */
    public void complete(UUID winnerId, Instant when) {
        if (status != MatchStatus.ACTIVE) {
            throw new MatchAlreadyCompletedException(id);
        }
        this.winnerId = winnerId;
        this.status = MatchStatus.COMPLETED;
        this.completedAt = when;
    }

    public UUID getId() {
        return id;
    }

    public MatchStatus getStatus() {
        return status;
    }

    public String getRegion() {
        return region;
    }

    public int getSkillBucket() {
        return skillBucket;
    }

    public UUID getWinnerId() {
        return winnerId;
    }

    public long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }
}
