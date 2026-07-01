package dev.maliik.relaycore.matchmaking.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.maliik.relaycore.common.error.DuplicateResourceException;
import dev.maliik.relaycore.common.error.ResourceNotFoundException;
import dev.maliik.relaycore.matchmaking.domain.Ticket;
import dev.maliik.relaycore.matchmaking.domain.TicketStatus;
import dev.maliik.relaycore.matchmaking.repository.TicketRepository;
import dev.maliik.relaycore.matchmaking.web.dto.TicketView;

/**
 * The player-facing half of matchmaking: enqueue a ticket and look one up. Match formation is the
 * {@code MatchmakingWorker}'s job. Kept intentionally shallow — this module exists to be the event
 * source, so depth lives in the outbox and the inventory/leaderboard consumers.
 */
@Service
public class MatchmakingService {

    private final TicketRepository tickets;

    public MatchmakingService(TicketRepository tickets) {
        this.tickets = tickets;
    }

    /**
     * Enqueues the player for matchmaking. A player may hold only one open ticket at a time — the
     * friendly check here returns 409, and a partial unique index backstops it under a concurrent race.
     */
    @Transactional
    public TicketView enqueue(UUID playerId, String region, int skillBucket) {
        if (tickets.existsByPlayerIdAndStatus(playerId, TicketStatus.QUEUED)) {
            throw new DuplicateResourceException("open matchmaking ticket for player", playerId.toString());
        }
        return TicketView.from(tickets.save(Ticket.queue(playerId, region, skillBucket)));
    }

    @Transactional(readOnly = true)
    public TicketView get(UUID ticketId) {
        return tickets.findById(ticketId)
                .map(TicketView::from)
                .orElseThrow(() -> new ResourceNotFoundException("ticket", ticketId.toString()));
    }
}
