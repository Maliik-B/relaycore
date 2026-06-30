package dev.maliik.relaycore.inventory.web.dto;

import java.util.UUID;

/**
 * Outcome of a store purchase: the player's balance after the debit and the resulting item stack.
 */
public record PurchaseResult(UUID playerId, long balance, ItemView purchased) {
}
