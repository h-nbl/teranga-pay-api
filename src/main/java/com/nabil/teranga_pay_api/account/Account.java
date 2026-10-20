package com.nabil.teranga_pay_api.account;

import com.nabil.teranga_pay_api.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "accounts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    private long balance;

    @Version
    private Long version;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public Account(User user, Instant createdAt) {
        this.user = user;
        this.createdAt = createdAt;
    }

    public void credit(long amount) {
        requirePositive(amount);
        this.balance = Math.addExact(this.balance, amount);
    }

    public void debit(long amount) {
        requirePositive(amount);
        if (amount > this.balance) {
            throw new IllegalStateException("Solde insuffisant");
        }
        this.balance -= amount;
    }

    private static void requirePositive(long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Le montant doit être strictement positif");
        }
    }
}
