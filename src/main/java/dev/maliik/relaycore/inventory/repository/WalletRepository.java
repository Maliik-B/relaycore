package dev.maliik.relaycore.inventory.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import dev.maliik.relaycore.inventory.domain.Wallet;

public interface WalletRepository extends JpaRepository<Wallet, UUID> {
}
