package dev.maliik.relaycore.matchmaking.domain;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * A player's request to be matched, parked in the queue by {@code region} and {@code skillBucket} until
 * the {@code MatchmakingWorker} groups it into a match. {@code playerId} is an opaque reference into the
 * players module (no cross-module FK). A partial unique index enforces at most one {@code QUEUED} ticket
 * per player; {@code @Version} guards the ticket against concurrent updates.
 */
@Entity
@Table(name = "matchmaking_tickets")
@EntityListeners(AuditingEntityListener.class)
public class Ticket {

    @Id
    private UUID id;

    @Column(name = "player_id", nullable = false)
    private UUID playerId;

    @Column(nullable = false, length = 32)
    private String region;

    @Column(name = "skill_bucket", nullable = false)
    private int skillBucket;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TicketStatus status;

    @Column(name = "match_id")
    private UUID matchId;

    @Version
    @Column(nullable = false)
    private long version;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Ticket() {
    }

    public static Ticket queue(UUID playerId, String region, int skillBucket) {
        Ticket ticket = new Ticket();
        ticket.id = UUID.randomUUID();
        ticket.playerId = playerId;
        ticket.region = region;
        ticket.skillBucket = skillBucket;
        ticket.status = TicketStatus.QUEUED;
        return ticket;
    }

    /** Marks the ticket consumed by the worker into the given match. */
    public void markMatched(UUID matchId) {
        this.status = TicketStatus.MATCHED;
        this.matchId = matchId;
    }

    public UUID getId() {
        return id;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public String getRegion() {
        return region;
    }

    public int getSkillBucket() {
        return skillBucket;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public UUID getMatchId() {
        return matchId;
    }

    public long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
