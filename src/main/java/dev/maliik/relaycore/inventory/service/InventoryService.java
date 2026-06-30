package dev.maliik.relaycore.inventory.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.maliik.relaycore.common.error.ResourceNotFoundException;
import dev.maliik.relaycore.common.idempotency.IdempotencyService;
import dev.maliik.relaycore.common.idempotency.IdempotentOutcome;
import dev.maliik.relaycore.inventory.config.InventoryCacheConfig;
import dev.maliik.relaycore.inventory.domain.InventoryItem;
import dev.maliik.relaycore.inventory.domain.Wallet;
import dev.maliik.relaycore.inventory.repository.CatalogItemRepository;
import dev.maliik.relaycore.inventory.repository.InventoryItemRepository;
import dev.maliik.relaycore.inventory.repository.WalletRepository;
import dev.maliik.relaycore.inventory.web.dto.InventoryView;
import dev.maliik.relaycore.inventory.web.dto.ItemView;

/**
 * Owns a player's wallet and item stacks. Reads are cache-aside ({@link Cacheable}); grants are
 * idempotent (replayed by {@link IdempotencyService} keyed on the {@code Idempotency-Key}) and evict
 * the cache. The wallet/item mutators are reused by {@code StoreService} for purchases, and the grant
 * entry point is shaped for reuse by the M3 {@code match-completed} consumer.
 */
@Service
public class InventoryService {

    private static final String GRANT_SCOPE = "inventory-grant";

    private final WalletRepository wallets;
    private final InventoryItemRepository inventoryItems;
    private final CatalogItemRepository catalogItems;
    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;

    public InventoryService(WalletRepository wallets, InventoryItemRepository inventoryItems,
            CatalogItemRepository catalogItems, IdempotencyService idempotencyService, ObjectMapper objectMapper) {
        this.wallets = wallets;
        this.inventoryItems = inventoryItems;
        this.catalogItems = catalogItems;
        this.idempotencyService = idempotencyService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = InventoryCacheConfig.INVENTORY_CACHE, key = "#playerId")
    public InventoryView getInventory(UUID playerId) {
        return buildView(playerId, wallets.findById(playerId).orElse(null));
    }

    /**
     * Grants currency and/or items, exactly once per idempotency key. The whole grant runs inside the
     * idempotency service's transaction, so a replay returns the original result without re-applying.
     */
    @CacheEvict(cacheNames = InventoryCacheConfig.INVENTORY_CACHE, key = "#playerId")
    public IdempotentOutcome<InventoryView> grant(UUID playerId, GrantCommand command, String idempotencyKey) {
        if (command.isEmpty()) {
            throw new IllegalArgumentException("a grant must include currency or at least one item");
        }
        return idempotencyService.process(GRANT_SCOPE, idempotencyKey, InventoryView.class,
                requestHash(playerId, command), () -> applyGrant(playerId, command));
    }

    private InventoryView applyGrant(UUID playerId, GrantCommand command) {
        Wallet wallet = getOrCreateWallet(playerId);
        if (command.currency() > 0) {
            wallet.credit(command.currency());
        }
        for (GrantItem item : command.items()) {
            if (!catalogItems.existsById(item.itemId())) {
                throw new ResourceNotFoundException("catalog item", item.itemId());
            }
            creditItem(playerId, item.itemId(), item.quantity());
        }
        return buildView(playerId, wallet);
    }

    Wallet getOrCreateWallet(UUID playerId) {
        return wallets.findById(playerId).orElseGet(() -> wallets.save(Wallet.forPlayer(playerId)));
    }

    int creditItem(UUID playerId, String itemId, int quantity) {
        InventoryItem existing = inventoryItems.findByPlayerIdAndItemId(playerId, itemId).orElse(null);
        if (existing == null) {
            return inventoryItems.save(InventoryItem.create(playerId, itemId, quantity)).getQuantity();
        }
        existing.addQuantity(quantity);
        return existing.getQuantity();
    }

    private InventoryView buildView(UUID playerId, Wallet wallet) {
        long balance = wallet == null ? 0L : wallet.getBalance();
        List<ItemView> items = inventoryItems.findByPlayerIdOrderByItemId(playerId).stream()
                .map(ItemView::from)
                .toList();
        return new InventoryView(playerId, balance, items);
    }

    private String requestHash(UUID playerId, GrantCommand command) {
        try {
            byte[] payload = (playerId + ":" + objectMapper.writeValueAsString(command))
                    .getBytes(StandardCharsets.UTF_8);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(payload));
        } catch (JsonProcessingException | NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Failed to fingerprint grant request", ex);
        }
    }
}
