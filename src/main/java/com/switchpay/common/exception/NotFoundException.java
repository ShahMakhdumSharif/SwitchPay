package com.switchpay.common.exception;

import org.springframework.http.HttpStatus;

public class NotFoundException extends SwitchPayException {

    public NotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
