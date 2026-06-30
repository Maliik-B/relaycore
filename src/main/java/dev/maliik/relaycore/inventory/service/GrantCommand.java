package dev.maliik.relaycore.inventory.service;

import java.util.List;

/**
 * Service-layer command describing a grant: an amount of currency and a list of item stacks to add.
 * Decouples the inventory service from the web request shape (and, later, from Kafka event shapes).
 */
public record GrantCommand(long currency, List<GrantItem> items) {

    public boolean isEmpty() {
        return currency <= 0 && (items == null || items.isEmpty());
    }
}
