package com.switchpay.common.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedException extends SwitchPayException {

    public UnauthorizedException(String message) {
        super(HttpStatus.UNAUTHORIZED, message);
    }
}
