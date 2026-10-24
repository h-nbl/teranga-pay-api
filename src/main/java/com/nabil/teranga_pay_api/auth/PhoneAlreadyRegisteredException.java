package com.nabil.teranga_pay_api.auth;

import com.nabil.teranga_pay_api.common.ApiException;
import org.springframework.http.HttpStatus;

public class PhoneAlreadyRegisteredException extends ApiException {

    public PhoneAlreadyRegisteredException() {
        super(HttpStatus.CONFLICT, "Un compte existe déjà avec ce numéro de téléphone");
    }
}