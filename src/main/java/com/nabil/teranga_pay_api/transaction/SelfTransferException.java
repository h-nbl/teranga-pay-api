package com.nabil.teranga_pay_api.transaction;

import com.nabil.teranga_pay_api.common.ApiException;
import org.springframework.http.HttpStatus;

public class SelfTransferException extends ApiException {

    public SelfTransferException() {
        super(HttpStatus.UNPROCESSABLE_CONTENT, "Vous ne pouvez pas vous envoyer de l'argent à vous-même");
    }
}