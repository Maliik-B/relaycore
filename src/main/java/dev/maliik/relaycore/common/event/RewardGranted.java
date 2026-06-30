package dev.maliik.relaycore.common.event;

import java.util.List;
import java.util.UUID;

/**
 * Event contract: a match reward (currency and/or items) has been granted to a player. Produced by
 * inventory (via the outbox) onto {@link Topics#REWARD_GRANTED} in the same transaction as the grant
 * itself, so the emission is atomic with the balance change.
 */
public record RewardGranted(UUID eventId, UUID matchId, UUID playerId, long currency, List<RewardItem> items) {

    /** A single granted item stack within a {@link RewardGranted} event. */
    public record RewardItem(String itemId, int quantity) {
    }
}
