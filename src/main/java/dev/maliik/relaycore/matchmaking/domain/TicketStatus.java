package dev.maliik.relaycore.matchmaking.domain;

/**
 * Lifecycle of a matchmaking ticket: {@code QUEUED} while waiting in the pool, then {@code MATCHED} once
 * the worker groups it into a formed match (recording the match id on the ticket).
 */
public enum TicketStatus {
    QUEUED,
    MATCHED
}
