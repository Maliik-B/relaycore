package dev.maliik.relaycore.inventory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.maliik.relaycore.common.idempotency.IdempotencyService;
import dev.maliik.relaycore.common.idempotency.IdempotentOutcome;
import dev.maliik.relaycore.common.outbox.OutboxAppender;
import dev.maliik.relaycore.inventory.domain.InventoryItem;
import dev.maliik.relaycore.inventory.domain.Wallet;
import dev.maliik.relaycore.inventory.repository.CatalogItemRepository;
import dev.maliik.relaycore.inventory.repository.InventoryItemRepository;
import dev.maliik.relaycore.inventory.repository.WalletRepository;
import dev.maliik.relaycore.inventory.web.dto.InventoryView;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private WalletRepository wallets;

    @Mock
    private InventoryItemRepository inventoryItems;

    @Mock
    private CatalogItemRepository catalogItems;

    @Mock
    private IdempotencyService idempotencyService;

    @Mock
    private OutboxAppender outboxAppender;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private InventoryService service;

    private final UUID player = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new InventoryService(wallets, inventoryItems, catalogItems, idempotencyService,
                outboxAppender, objectMapper);
    }

    @Test
    void getInventoryBuildsViewFromWalletAndItems() {
        Wallet wallet = Wallet.forPlayer(player);
        wallet.credit(300);
        when(wallets.findById(player)).thenReturn(Optional.of(wallet));
        when(inventoryItems.findByPlayerIdOrderByItemId(player))
                .thenReturn(List.of(InventoryItem.create(player, "sword_iron", 2)));

        InventoryView view = service.getInventory(player);

        assertThat(view.balance()).isEqualTo(300);
        assertThat(view.items()).hasSize(1);
        assertThat(view.items().get(0).itemId()).isEqualTo("sword_iron");
        assertThat(view.items().get(0).quantity()).isEqualTo(2);
    }

    @Test
    void emptyGrantIsRejectedBeforeTouchingIdempotency() {
        assertThatThrownBy(() -> service.grant(player, new GrantCommand(0, List.of()), "key-1"))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(idempotencyService);
    }

    @Test
    void grantCreditsCurrencyThroughTheIdempotencyService() {
        // Make the idempotency service run the supplied action (as a fresh, non-replayed execution).
        when(idempotencyService.process(eq("inventory-grant"), eq("key-1"), eq(InventoryView.class), anyString(), any()))
                .thenAnswer(invocation -> {
                    Supplier<InventoryView> action = invocation.getArgument(4);
                    return new IdempotentOutcome<>(action.get(), false);
                });
        when(wallets.findById(player)).thenReturn(Optional.empty());
        when(wallets.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(inventoryItems.findByPlayerIdOrderByItemId(player)).thenReturn(List.of());

        IdempotentOutcome<InventoryView> outcome =
                service.grant(player, new GrantCommand(100, List.of()), "key-1");

        assertThat(outcome.replayed()).isFalse();
        assertThat(outcome.result().balance()).isEqualTo(100);
        assertThat(outcome.result().items()).isEmpty();
    }
}
