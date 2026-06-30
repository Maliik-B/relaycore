package dev.maliik.relaycore.common.error;

/**
 * Thrown when a store purchase targets an item that exists but is not currently purchasable
 * (disabled in the catalog). Mapped to HTTP 409.
 */
public class ItemNotPurchasableException extends RuntimeException {

    public ItemNotPurchasableException(String itemId) {
        super("item is not purchasable: " + itemId);
    }
}
