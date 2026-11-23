package com.switchpay.common.event;

import com.switchpay.common.enums.TransactionStatus;
import com.switchpay.common.enums.TransactionType;
import java.math.BigDecimal;
import java.time.Instant;

public class TransactionCompletedEvent {

    private final Long transactionId;
    private final String transactionRef;
    private final TransactionType type;
    private final String channelCode;
    private final TransactionStatus status;
    private final BigDecimal amount;
    private final String currency;
    private final String sourceAccountNo;
    private final Long sourceAccountId;
    private final String destinationAccountNo;
    private final Long destinationAccountId;
    private final Long merchantId;
    private final Long checkoutSessionId;
    private final Instant occurredAt;

    public TransactionCompletedEvent(
            Long transactionId,
            String transactionRef,
            TransactionType type,
            String channelCode,
            TransactionStatus status,
            BigDecimal amount,
            String currency,
            String sourceAccountNo,
            Long sourceAccountId,
            String destinationAccountNo,
            Long destinationAccountId,
            Long merchantId,
            Long checkoutSessionId,
            Instant occurredAt) {
        this.transactionId = transactionId;
        this.transactionRef = transactionRef;
        this.type = type;
        this.channelCode = channelCode;
        this.status = status;
        this.amount = amount;
        this.currency = currency;
        this.sourceAccountNo = sourceAccountNo;
        this.sourceAccountId = sourceAccountId;
        this.destinationAccountNo = destinationAccountNo;
        this.destinationAccountId = destinationAccountId;
        this.merchantId = merchantId;
        this.checkoutSessionId = checkoutSessionId;
        this.occurredAt = occurredAt;
    }

    public Long getTransactionId() {
        return transactionId;
    }

    public String getTransactionRef() {
        return transactionRef;
    }

    public TransactionType getType() {
        return type;
    }

    public String getChannelCode() {
        return channelCode;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getSourceAccountNo() {
        return sourceAccountNo;
    }

    public Long getSourceAccountId() {
        return sourceAccountId;
    }

    public String getDestinationAccountNo() {
        return destinationAccountNo;
    }

    public Long getDestinationAccountId() {
        return destinationAccountId;
    }

    public Long getMerchantId() {
        return merchantId;
    }

    public Long getCheckoutSessionId() {
        return checkoutSessionId;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
