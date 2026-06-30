package dev.maliik.relaycore.inventory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A purchasable/grantable item definition: the item registry for the inventory module. Reference
 * data, seeded by Flyway. {@code enabled} gates store purchase (disabled items can still be granted).
 */
@Entity
@Table(name = "catalog_items")
public class CatalogItem {

    @Id
    @Column(name = "item_id", length = 64)
    private String itemId;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(nullable = false)
    private long price;

    @Column(nullable = false)
    private boolean enabled;

    protected CatalogItem() {
    }

    public String getItemId() {
        return itemId;
    }

    public String getName() {
        return name;
    }

    public long getPrice() {
        return price;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
