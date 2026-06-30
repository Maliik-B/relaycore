package dev.maliik.relaycore.inventory.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import dev.maliik.relaycore.inventory.domain.CatalogItem;

public interface CatalogItemRepository extends JpaRepository<CatalogItem, String> {

    List<CatalogItem> findByEnabledTrueOrderByPriceAsc();
}
