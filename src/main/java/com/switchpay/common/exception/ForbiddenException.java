package com.switchpay.common.exception;

import org.springframework.http.HttpStatus;

public class ForbiddenException extends SwitchPayException {

    public ForbiddenException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }
}
