/**
 * Matchmaking module: skill/region queue, a background worker that forms matches, and match
 * lifecycle. Deliberately shallow (bucket matcher, not ELO) — its job is to be the event SOURCE,
 * emitting {@code match-completed} to Kafka.
 */
package dev.maliik.relaycore.matchmaking;
