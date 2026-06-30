package dev.maliik.relaycore.inventory.web;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.maliik.relaycore.common.security.AuthenticatedPlayer;
import dev.maliik.relaycore.inventory.service.StoreService;
import dev.maliik.relaycore.inventory.web.dto.CatalogItemView;
import dev.maliik.relaycore.inventory.web.dto.PurchaseRequest;
import dev.maliik.relaycore.inventory.web.dto.PurchaseResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/store")
@Tag(name = "Store", description = "Catalog and purchases (optimistic-locked, double-spend safe).")
@SecurityRequirement(name = "bearerAuth")
public class StoreController {

    private final StoreService storeService;

    public StoreController(StoreService storeService) {
        this.storeService = storeService;
    }

    @GetMapping("/catalog")
    @Operation(summary = "List purchasable catalog items.")
    public List<CatalogItemView> catalog() {
        return storeService.catalog();
    }

    @PostMapping("/purchase")
    @Operation(summary = "Buy an item for the authenticated player, spending soft currency.")
    public PurchaseResult purchase(@AuthenticationPrincipal AuthenticatedPlayer principal,
            @Valid @RequestBody PurchaseRequest request) {
        return storeService.purchase(principal.id(), request.itemId(), request.quantity());
    }
}
