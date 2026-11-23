package com.switchpay.common.exception;

import org.springframework.http.HttpStatus;

public class SwitchPayException extends RuntimeException {

    private final HttpStatus status;

    public SwitchPayException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
