package com.nabil.teranga_pay_api.account;

import com.nabil.teranga_pay_api.user.User;

import java.time.Instant;

public record AccountResponse(String fullName, String phone, long balance,
                              String currency, Instant createdAt) {

    public static final String CURRENCY = "XOF";

    static AccountResponse from(Account account) {
        User user = account.getUser();
        return new AccountResponse(user.getFullName(), user.getPhone(),
                account.getBalance(), CURRENCY, account.getCreatedAt());
    }
}