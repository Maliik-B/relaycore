package dev.maliik.relaycore.matchmaking.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import dev.maliik.relaycore.matchmaking.domain.Ticket;
import dev.maliik.relaycore.matchmaking.domain.TicketStatus;

public interface TicketRepository extends JpaRepository<Ticket, UUID> {

    /** Oldest-first batch of open tickets for the worker to group into matches (backed by a partial index). */
    List<Ticket> findByStatusOrderByCreatedAtAsc(TicketStatus status, Pageable pageable);

    /** Whether the player already has a ticket in the given state — the friendly guard on double-queueing. */
    boolean existsByPlayerIdAndStatus(UUID playerId, TicketStatus status);
}
