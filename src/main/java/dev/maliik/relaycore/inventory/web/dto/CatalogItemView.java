package dev.maliik.relaycore.inventory.web.dto;

import dev.maliik.relaycore.inventory.domain.CatalogItem;

public record CatalogItemView(String itemId, String name, long price) {

    public static CatalogItemView from(CatalogItem item) {
        return new CatalogItemView(item.getItemId(), item.getName(), item.getPrice());
    }
}
