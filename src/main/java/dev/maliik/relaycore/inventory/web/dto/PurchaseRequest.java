package dev.maliik.relaycore.inventory.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record PurchaseRequest(
        @NotBlank String itemId,
        @Positive int quantity) {
}
