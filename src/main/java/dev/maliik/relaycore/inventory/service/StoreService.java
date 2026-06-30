package dev.maliik.relaycore.inventory.service;

import java.util.List;
import java.util.UUID;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.maliik.relaycore.common.error.ItemNotPurchasableException;
import dev.maliik.relaycore.common.error.ResourceNotFoundException;
import dev.maliik.relaycore.inventory.config.InventoryCacheConfig;
import dev.maliik.relaycore.inventory.domain.CatalogItem;
import dev.maliik.relaycore.inventory.domain.Wallet;
import dev.maliik.relaycore.inventory.repository.CatalogItemRepository;
import dev.maliik.relaycore.inventory.web.dto.CatalogItemView;
import dev.maliik.relaycore.inventory.web.dto.ItemView;
import dev.maliik.relaycore.inventory.web.dto.PurchaseResult;

/**
 * Store catalog and purchases. A purchase debits the wallet and credits the item in one transaction;
 * the wallet's optimistic lock ({@code @Version}) means concurrent buys can't double-spend — a losing
 * writer fails with an optimistic-lock conflict (surfaced as 409) rather than overdrawing the balance.
 */
@Service
public class StoreService {

    private final CatalogItemRepository catalogItems;
    private final InventoryService inventoryService;

    public StoreService(CatalogItemRepository catalogItems, InventoryService inventoryService) {
        this.catalogItems = catalogItems;
        this.inventoryService = inventoryService;
    }

    @Transactional(readOnly = true)
    public List<CatalogItemView> catalog() {
        return catalogItems.findByEnabledTrueOrderByPriceAsc().stream()
                .map(CatalogItemView::from)
                .toList();
    }

    @Transactional
    @CacheEvict(cacheNames = InventoryCacheConfig.INVENTORY_CACHE, key = "#playerId")
    public PurchaseResult purchase(UUID playerId, String itemId, int quantity) {
        CatalogItem item = catalogItems.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("catalog item", itemId));
        if (!item.isEnabled()) {
            throw new ItemNotPurchasableException(itemId);
        }
        long cost = Math.multiplyExact(item.getPrice(), quantity);

        Wallet wallet = inventoryService.getOrCreateWallet(playerId);
        wallet.debit(cost);
        int newQuantity = inventoryService.creditItem(playerId, itemId, quantity);

        return new PurchaseResult(playerId, wallet.getBalance(), new ItemView(itemId, newQuantity));
    }
}
