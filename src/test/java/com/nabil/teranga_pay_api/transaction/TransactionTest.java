package com.nabil.teranga_pay_api.transaction;

import com.nabil.teranga_pay_api.account.Account;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransactionTest {

    private static final Instant NOW = Instant.parse("2026-10-30T10:00:00Z");

    private final Account from = new Account(null, NOW);
    private final Account to = new Account(null, NOW);

    @Test
    void transferRecordsAccountsAmountAndIdempotencyData() {
        Transaction tx = Transaction.transfer(from, to, 25_000, "key-1", "hash-1", NOW);

        assertThat(tx.getType()).isEqualTo(TransactionType.TRANSFER);
        assertThat(tx.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
        assertThat(tx.getFromAccount()).isSameAs(from);
        assertThat(tx.getToAccount()).isSameAs(to);
        assertThat(tx.getAmount()).isEqualTo(25_000);
        assertThat(tx.getIdempotencyKey()).isEqualTo("key-1");
        assertThat(tx.getRequestHash()).isEqualTo("hash-1");
        assertThat(tx.getRiskScore()).isZero();
        assertThat(tx.getCreatedAt()).isEqualTo(NOW);
    }

    @Test
    void transferRefusesNonPositiveAmounts() {
        assertThatThrownBy(() -> Transaction.transfer(from, to, 0, "key-1", "hash-1", NOW))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Transaction.transfer(from, to, -5, "key-1", "hash-1", NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void transferRequiresAnIdempotencyKeyAndRequestHash() {
        assertThatThrownBy(() -> Transaction.transfer(from, to, 1_000, null, "hash-1", NOW))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Transaction.transfer(from, to, 1_000, "key-1", null, NOW))
                .isInstanceOf(NullPointerException.class);
    }
}