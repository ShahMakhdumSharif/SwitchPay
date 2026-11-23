package com.switchpay.common.dto;

import com.switchpay.common.enums.TransactionStatus;
import java.math.BigDecimal;

public class HandlerResult {

    private final TransactionStatus status;
    private final String declineReason;
    private final BigDecimal balance;

    public HandlerResult(TransactionStatus status, String declineReason, BigDecimal balance) {
        this.status = status;
        this.declineReason = declineReason;
        this.balance = balance;
    }

    public static HandlerResult approved() {
        return new HandlerResult(TransactionStatus.APPROVED, null, null);
    }

    public static HandlerResult approvedWithBalance(BigDecimal balance) {
        return new HandlerResult(TransactionStatus.APPROVED, null, balance);
    }

    public static HandlerResult declined(String reason) {
        return new HandlerResult(TransactionStatus.DECLINED, reason, null);
    }

    public static HandlerResult failed(String reason) {
        return new HandlerResult(TransactionStatus.FAILED, reason, null);
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public String getDeclineReason() {
        return declineReason;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public boolean isApproved() {
        return status == TransactionStatus.APPROVED;
    }
}
