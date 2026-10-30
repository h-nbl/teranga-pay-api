package com.nabil.teranga_pay_api.transaction;

import com.nabil.teranga_pay_api.common.ApiException;
import org.springframework.http.HttpStatus;

public class InvalidDateRangeException extends ApiException {

    public InvalidDateRangeException() {
        super(HttpStatus.BAD_REQUEST, "La date de début doit être antérieure à la date de fin");
    }
}