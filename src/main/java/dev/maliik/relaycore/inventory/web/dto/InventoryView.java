package dev.maliik.relaycore.inventory.web.dto;

import java.util.List;
import java.util.UUID;

/**
 * A player's inventory snapshot: currency balance plus owned item stacks. The cached, replay-stored
 * read model for the inventory endpoints.
 */
public record InventoryView(UUID playerId, long balance, List<ItemView> items) {
}
