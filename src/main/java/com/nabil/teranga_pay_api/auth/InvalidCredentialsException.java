package com.nabil.teranga_pay_api.auth;

import com.nabil.teranga_pay_api.common.ApiException;
import org.springframework.http.HttpStatus;

public class InvalidCredentialsException extends ApiException {

    public InvalidCredentialsException() {
        super(HttpStatus.UNAUTHORIZED, "Numéro de téléphone ou mot de passe incorrect");
    }
}