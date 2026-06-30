package dev.maliik.relaycore.inventory.domain;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * One stack of an owned item: a (player, item) pair with a quantity. The unique (player_id, item_id)
 * constraint keeps it to a single row per item per player; {@code @Version} guards concurrent
 * quantity changes.
 */
@Entity
@Table(name = "inventory_items")
@EntityListeners(AuditingEntityListener.class)
public class InventoryItem {

    @Id
    private UUID id;

    @Column(name = "player_id", nullable = false)
    private UUID playerId;

    @Column(name = "item_id", nullable = false, length = 64)
    private String itemId;

    @Column(nullable = false)
    private int quantity;

    @Version
    @Column(nullable = false)
    private long version;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected InventoryItem() {
    }

    public static InventoryItem create(UUID playerId, String itemId, int quantity) {
        InventoryItem item = new InventoryItem();
        item.id = UUID.randomUUID();
        item.playerId = playerId;
        item.itemId = itemId;
        item.quantity = quantity;
        return item;
    }

    public void addQuantity(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("quantity to add must be non-negative");
        }
        quantity += amount;
    }

    public UUID getId() {
        return id;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public String getItemId() {
        return itemId;
    }

    public int getQuantity() {
        return quantity;
    }
}
