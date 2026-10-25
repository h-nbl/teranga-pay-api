package com.nabil.teranga_pay_api.account;

import com.nabil.teranga_pay_api.common.ApiException;
import org.springframework.http.HttpStatus;

public class DepositLimitExceededException extends ApiException {

    public DepositLimitExceededException(long maxDeposit) {
        super(HttpStatus.UNPROCESSABLE_CONTENT,
                "Le montant dépasse le plafond de %d XOF par dépôt".formatted(maxDeposit));
    }
}