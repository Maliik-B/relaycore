package dev.maliik.relaycore.inventory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.maliik.relaycore.common.error.InsufficientFundsException;
import dev.maliik.relaycore.common.error.ItemNotPurchasableException;
import dev.maliik.relaycore.common.error.ResourceNotFoundException;
import dev.maliik.relaycore.inventory.domain.CatalogItem;
import dev.maliik.relaycore.inventory.domain.Wallet;
import dev.maliik.relaycore.inventory.repository.CatalogItemRepository;
import dev.maliik.relaycore.inventory.web.dto.PurchaseResult;

@ExtendWith(MockitoExtension.class)
class StoreServiceTest {

    @Mock
    private CatalogItemRepository catalogItems;

    @Mock
    private InventoryService inventoryService;

    @InjectMocks
    private StoreService storeService;

    private final UUID player = UUID.randomUUID();

    @Test
    void purchaseDebitsWalletAndCreditsItem() {
        CatalogItem item = mock(CatalogItem.class);
        when(item.isEnabled()).thenReturn(true);
        when(item.getPrice()).thenReturn(100L);
        when(catalogItems.findById("sword_iron")).thenReturn(Optional.of(item));
        Wallet wallet = Wallet.forPlayer(player);
        wallet.credit(500);
        when(inventoryService.getOrCreateWallet(player)).thenReturn(wallet);
        when(inventoryService.creditItem(player, "sword_iron", 2)).thenReturn(2);

        PurchaseResult result = storeService.purchase(player, "sword_iron", 2);

        assertThat(result.balance()).isEqualTo(300);
        assertThat(result.purchased().quantity()).isEqualTo(2);
        assertThat(wallet.getBalance()).isEqualTo(300);
    }

    @Test
    void purchaseWithInsufficientFundsThrowsAndGrantsNothing() {
        CatalogItem item = mock(CatalogItem.class);
        when(item.isEnabled()).thenReturn(true);
        when(item.getPrice()).thenReturn(100L);
        when(catalogItems.findById("sword_iron")).thenReturn(Optional.of(item));
        Wallet wallet = Wallet.forPlayer(player);
        wallet.credit(50);
        when(inventoryService.getOrCreateWallet(player)).thenReturn(wallet);

        assertThatThrownBy(() -> storeService.purchase(player, "sword_iron", 1))
                .isInstanceOf(InsufficientFundsException.class);
        verify(inventoryService, never()).creditItem(any(), any(), anyInt());
    }

    @Test
    void purchaseOfDisabledItemIsRejected() {
        CatalogItem item = mock(CatalogItem.class);
        when(item.isEnabled()).thenReturn(false);
        when(catalogItems.findById("legacy_banner")).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> storeService.purchase(player, "legacy_banner", 1))
                .isInstanceOf(ItemNotPurchasableException.class);
    }

    @Test
    void purchaseOfUnknownItemIsRejected() {
        when(catalogItems.findById("nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> storeService.purchase(player, "nope", 1))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
