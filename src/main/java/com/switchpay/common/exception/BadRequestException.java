package com.switchpay.common.exception;

import org.springframework.http.HttpStatus;

public class BadRequestException extends SwitchPayException {

    public BadRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
