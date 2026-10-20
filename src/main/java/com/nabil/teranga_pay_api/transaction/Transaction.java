package com.nabil.teranga_pay_api.transaction;

import com.nabil.teranga_pay_api.account.Account;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transactions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private  TransactionType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_account_id")
    private Account fromAccount;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "to_account_id", nullable = false)
    private Account toAccount;

    @Column(nullable = false)
    private long amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionStatus status;

    @Column(length = 80)
    private String idempotencyKey;

    @Column(length = 64)
    private String requestHash;

    @Column(nullable = false)
    private int riskScore;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public Transaction(TransactionType type, Account fromAccount, Account toAccount, long amount, TransactionStatus status, String idempotencyKey, String requestHash, int riskScore, Instant createdAt) {
        this.type = type;
        this.fromAccount = fromAccount;
        this.toAccount = toAccount;
        this.amount = amount;
        this.status = status;
        this.idempotencyKey = idempotencyKey;
        this.requestHash = requestHash;
        this.riskScore = riskScore;
        this.createdAt = createdAt;
    }
}


