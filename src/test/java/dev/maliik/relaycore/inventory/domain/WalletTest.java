package dev.maliik.relaycore.inventory.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import dev.maliik.relaycore.common.error.InsufficientFundsException;

class WalletTest {

    @Test
    void creditsAndDebits() {
        Wallet wallet = Wallet.forPlayer(UUID.randomUUID());

        wallet.credit(100);
        wallet.debit(30);

        assertThat(wallet.getBalance()).isEqualTo(70);
    }

    @Test
    void debitBeyondBalanceThrowsAndLeavesBalanceUnchanged() {
        Wallet wallet = Wallet.forPlayer(UUID.randomUUID());
        wallet.credit(50);

        assertThatThrownBy(() -> wallet.debit(51)).isInstanceOf(InsufficientFundsException.class);
        assertThat(wallet.getBalance()).isEqualTo(50);
    }

    @Test
    void negativeAmountsAreRejected() {
        Wallet wallet = Wallet.forPlayer(UUID.randomUUID());

        assertThatThrownBy(() -> wallet.credit(-1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> wallet.debit(-1)).isInstanceOf(IllegalArgumentException.class);
    }
}
