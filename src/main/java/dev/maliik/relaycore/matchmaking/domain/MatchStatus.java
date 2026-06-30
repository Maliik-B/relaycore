package dev.maliik.relaycore.matchmaking.domain;

/**
 * Match lifecycle. M3 only records {@code COMPLETED} results (matchmaking is the event source);
 * {@code FORMING}/{@code ACTIVE} are reserved for the queue/worker lifecycle added in M4.
 */
public enum MatchStatus {
    FORMING,
    ACTIVE,
    COMPLETED
}
