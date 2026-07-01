package dev.maliik.relaycore.matchmaking.web.dto;

import java.time.Instant;
import java.util.UUID;

import dev.maliik.relaycore.matchmaking.domain.Ticket;

/** Read model for a matchmaking ticket. {@code matchId} is populated once the worker matches it. */
public record TicketView(UUID id, UUID playerId, String region, int skillBucket, String status,
        UUID matchId, Instant createdAt) {

    public static TicketView from(Ticket ticket) {
        return new TicketView(ticket.getId(), ticket.getPlayerId(), ticket.getRegion(), ticket.getSkillBucket(),
                ticket.getStatus().name(), ticket.getMatchId(), ticket.getCreatedAt());
    }
}
