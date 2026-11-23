package com.switchpay.common.exception;

import org.springframework.http.HttpStatus;

public class ConflictException extends SwitchPayException {

    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
