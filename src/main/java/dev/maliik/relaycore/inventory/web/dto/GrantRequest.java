package dev.maliik.relaycore.inventory.web.dto;

import java.util.List;

import dev.maliik.relaycore.inventory.service.GrantCommand;
import dev.maliik.relaycore.inventory.service.GrantItem;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Positive;

/**
 * A grant of currency and/or items to a player — the shape a match-reward or admin grant takes.
 * Must be non-empty (enforced by the service); individual amounts are validated here.
 */
public record GrantRequest(
        @PositiveOrZero long currency,
        @Valid List<Item> items) {

    public record Item(
            @NotBlank String itemId,
            @Positive int quantity) {
    }

    public GrantCommand toCommand() {
        List<GrantItem> grantItems = items == null
                ? List.of()
                : items.stream().map(item -> new GrantItem(item.itemId(), item.quantity())).toList();
        return new GrantCommand(currency, grantItems);
    }
}
