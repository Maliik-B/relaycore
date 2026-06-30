package dev.maliik.relaycore.common.event;

/**
 * Kafka topic names for the cross-module event loop. Kept in {@code common} so producers and
 * consumers in different modules share one source of truth and never reach into each other.
 */
public final class Topics {

    /** Emitted by matchmaking when a match result is recorded; consumed by inventory to grant rewards. */
    public static final String MATCH_COMPLETED = "match-completed";

    /** Emitted by inventory after a match reward is granted; consumed by leaderboards (M4). */
    public static final String REWARD_GRANTED = "reward-granted";

    private Topics() {
    }
}
