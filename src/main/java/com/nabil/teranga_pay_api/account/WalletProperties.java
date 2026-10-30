package com.nabil.teranga_pay_api.account;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app.wallet")
@Validated
public record WalletProperties(@Min(1) long maxDeposit) {
}