package com.nabil.teranga_pay_api.account;

import java.time.Instant;
import java.util.UUID;

public record DepositResponse(UUID transactionId, long amount, long balance,
                              String currency, Instant createdAt) {
}