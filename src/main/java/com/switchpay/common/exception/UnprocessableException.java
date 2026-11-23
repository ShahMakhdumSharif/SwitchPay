package com.switchpay.common.exception;

import org.springframework.http.HttpStatus;

public class UnprocessableException extends SwitchPayException {

    public UnprocessableException(String message) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, message);
    }
}
