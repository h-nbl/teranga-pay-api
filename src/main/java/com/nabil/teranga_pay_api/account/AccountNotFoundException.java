package com.nabil.teranga_pay_api.account;

import com.nabil.teranga_pay_api.common.ApiException;
import org.springframework.http.HttpStatus;

public class AccountNotFoundException extends ApiException {

    public AccountNotFoundException() {
        super(HttpStatus.NOT_FOUND, "Compte introuvable");
    }
}