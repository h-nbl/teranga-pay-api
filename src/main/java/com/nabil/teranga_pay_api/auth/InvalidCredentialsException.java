package com.nabil.teranga_pay_api.auth;

public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Numéro de téléphone ou mot de passe incorrect");
    }
}