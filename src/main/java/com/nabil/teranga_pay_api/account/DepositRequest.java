package com.nabil.teranga_pay_api.account;

import jakarta.validation.constraints.Positive;

public record DepositRequest(@Positive long amount) {
}