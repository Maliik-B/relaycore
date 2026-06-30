package dev.maliik.relaycore.inventory.web.dto;

import dev.maliik.relaycore.inventory.domain.InventoryItem;

public record ItemView(String itemId, int quantity) {

    public static ItemView from(InventoryItem item) {
        return new ItemView(item.getItemId(), item.getQuantity());
    }
}
