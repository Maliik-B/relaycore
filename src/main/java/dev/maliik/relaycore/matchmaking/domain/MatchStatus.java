package dev.maliik.relaycore.matchmaking.domain;

/**
 * Match lifecycle. The worker forms matches directly as {@code ACTIVE}; reporting a result moves them to
 * {@code COMPLETED}. {@code FORMING} is reserved for a future multi-stage formation flow and is unused today.
 */
public enum MatchStatus {
    FORMING,
    ACTIVE,
    COMPLETED
}
