package com.nabil.teranga_pay_api.transaction;

import com.nabil.teranga_pay_api.common.ApiException;
import org.springframework.http.HttpStatus;

public class RecipientNotFoundException extends ApiException {

    public RecipientNotFoundException() {
        super(HttpStatus.NOT_FOUND, "Destinataire introuvable");
    }
}