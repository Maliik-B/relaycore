/**
 * Leaderboard module: ranked queries (top-N, player rank, neighbors) backed by Redis sorted sets.
 * Consumes the event stream ({@code match-completed} / {@code reward-granted}) to update rankings.
 * The visible payoff of the Kafka event loop.
 */
package dev.maliik.relaycore.leaderboard;
