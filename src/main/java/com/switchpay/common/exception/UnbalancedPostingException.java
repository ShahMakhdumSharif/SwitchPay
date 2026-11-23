package com.switchpay.common.exception;

public class UnbalancedPostingException extends UnprocessableException {

    public UnbalancedPostingException(String message) {
        super(message);
    }
}
