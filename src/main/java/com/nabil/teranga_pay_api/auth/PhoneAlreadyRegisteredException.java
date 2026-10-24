package com.nabil.teranga_pay_api.auth;

public class PhoneAlreadyRegisteredException extends RuntimeException {

    public PhoneAlreadyRegisteredException() {
        super("Un compte existe déjà avec ce numéro de téléphone");
    }
}