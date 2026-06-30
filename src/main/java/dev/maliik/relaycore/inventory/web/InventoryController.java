package dev.maliik.relaycore.inventory.web;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.maliik.relaycore.common.idempotency.IdempotentOutcome;
import dev.maliik.relaycore.inventory.service.InventoryService;
import dev.maliik.relaycore.inventory.web.dto.GrantRequest;
import dev.maliik.relaycore.inventory.web.dto.InventoryView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/players/{playerId}/inventory")
@Tag(name = "Inventory", description = "Player wallet + item inventory; idempotent grants.")
@SecurityRequirement(name = "bearerAuth")
public class InventoryController {

    private static final String IDEMPOTENCY_REPLAYED_HEADER = "Idempotency-Replayed";

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    @Operation(summary = "A player's wallet balance and owned items.")
    public InventoryView get(@PathVariable UUID playerId) {
        return inventoryService.getInventory(playerId);
    }

    @PostMapping("/grant")
    @Operation(summary = "Grant currency and/or items to a player. Idempotent on the Idempotency-Key header.")
    public ResponseEntity<InventoryView> grant(@PathVariable UUID playerId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody GrantRequest request) {
        IdempotentOutcome<InventoryView> outcome =
                inventoryService.grant(playerId, request.toCommand(), idempotencyKey);
        return ResponseEntity.ok()
                .header(IDEMPOTENCY_REPLAYED_HEADER, String.valueOf(outcome.replayed()))
                .body(outcome.result());
    }
}
