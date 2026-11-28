package com.switchpay.adjustment.dto;

import jakarta.validation.constraints.NotBlank;

public class ReversalRequest {

    @NotBlank(message = "transactionRef is required")
    private String transactionRef;

    private String reason;

    public ReversalRequest() {
    }

    public ReversalRequest(String transactionRef, String reason) {
        this.transactionRef = transactionRef;
        this.reason = reason;
    }

    public String getTransactionRef() {
        return transactionRef;
    }

    public void setTransactionRef(String transactionRef) {
        this.transactionRef = transactionRef;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
