package com.nabil.teranga_pay_api.transaction;

import com.nabil.teranga_pay_api.common.ApiException;
import org.springframework.http.HttpStatus;

public class InsufficientFundsException extends ApiException {

    public InsufficientFundsException() {
        super(HttpStatus.UNPROCESSABLE_CONTENT, "Solde insuffisant");
    }
}