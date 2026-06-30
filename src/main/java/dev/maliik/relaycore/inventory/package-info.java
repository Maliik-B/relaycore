/**
 * Inventory &amp; store module (the depth domain): player inventory + soft currency, idempotent item
 * grants, store purchases with optimistic locking. Consumes {@code match-completed}, grants rewards,
 * emits {@code reward-granted}. Correctness under concurrency matters most here.
 */
package dev.maliik.relaycore.inventory;
