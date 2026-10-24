package com.nabil.teranga_pay_api.common;

public class InvalidPhoneNumberException extends IllegalArgumentException {

    public InvalidPhoneNumberException() {
        super("Numéro de téléphone invalide");
    }
}