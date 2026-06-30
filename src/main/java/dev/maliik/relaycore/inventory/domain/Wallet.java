package dev.maliik.relaycore.inventory.domain;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import dev.maliik.relaycore.common.error.InsufficientFundsException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * A player's soft-currency balance, keyed by player id (one wallet per player). The {@code @Version}
 * column gives optimistic locking so concurrent debits can't double-spend: a stale write fails on
 * commit rather than silently overwriting a balance someone else already changed.
 */
@Entity
@Table(name = "wallets")
@EntityListeners(AuditingEntityListener.class)
public class Wallet {

    @Id
    @Column(name = "player_id")
    private UUID playerId;

    @Column(nullable = false)
    private long balance;

    @Version
    @Column(nullable = false)
    private long version;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Wallet() {
    }

    public static Wallet forPlayer(UUID playerId) {
        Wallet wallet = new Wallet();
        wallet.playerId = playerId;
        wallet.balance = 0;
        return wallet;
    }

    public void credit(long amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("credit amount must be non-negative");
        }
        balance += amount;
    }

    public void debit(long amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("debit amount must be non-negative");
        }
        if (balance < amount) {
            throw new InsufficientFundsException(playerId, amount, balance);
        }
        balance -= amount;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public long getBalance() {
        return balance;
    }

    public long getVersion() {
        return version;
    }
}
