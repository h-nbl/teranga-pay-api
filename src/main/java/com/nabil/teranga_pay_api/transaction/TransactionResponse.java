package com.nabil.teranga_pay_api.transaction;

import java.time.Instant;
import java.util.UUID;

public record TransactionResponse(UUID id, TransactionType type, TransactionDirection direction,
                                  long amount, String currency, TransactionStatus status,
                                  Instant createdAt) {

    private static final String CURRENCY = "XOF"; // à factoriser plus tard avec les autres DTO

    public static TransactionResponse from(Transaction tx, UUID myAccountId) {
        boolean outgoing = tx.getFromAccount() != null
                && tx.getFromAccount().getId().equals(myAccountId);
        return new TransactionResponse(tx.getId(), tx.getType(),
                outgoing ? TransactionDirection.OUT : TransactionDirection.IN,
                tx.getAmount(), CURRENCY, tx.getStatus(), tx.getCreatedAt());
    }
}