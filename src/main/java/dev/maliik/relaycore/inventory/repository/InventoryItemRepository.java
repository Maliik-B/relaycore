package dev.maliik.relaycore.inventory.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import dev.maliik.relaycore.inventory.domain.InventoryItem;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, UUID> {

    List<InventoryItem> findByPlayerIdOrderByItemId(UUID playerId);

    Optional<InventoryItem> findByPlayerIdAndItemId(UUID playerId, String itemId);
}
