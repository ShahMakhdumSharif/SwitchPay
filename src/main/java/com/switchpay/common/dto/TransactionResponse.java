package com.switchpay.common.dto;

import com.switchpay.common.enums.TransactionStatus;
import com.switchpay.common.enums.TransactionType;
import java.math.BigDecimal;
import java.time.Instant;

public class TransactionResponse {

    private Long transactionId;
    private String reference;
    private String idempotencyKey;
    private TransactionType type;
    private String channelCode;
    private Long channelId;
    private TransactionStatus status;
    private BigDecimal amount;
    private String currency;
    private String sourceAccountNo;
    private Long sourceAccountId;
    private String destinationAccountNo;
    private Long destinationAccountId;
    private Long merchantId;
    private Long checkoutSessionId;
    private String declineReason;
    private BigDecimal balance;
    private Instant createdAt;
    private Instant updatedAt;

    public TransactionResponse() {
    }

    public static Builder builder() {
        return new Builder();
    }

    public Long getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public String getChannelCode() {
        return channelCode;
    }

    public void setChannelCode(String channelCode) {
        this.channelCode = channelCode;
    }

    public Long getChannelId() {
        return channelId;
    }

    public void setChannelId(Long channelId) {
        this.channelId = channelId;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getSourceAccountNo() {
        return sourceAccountNo;
    }

    public void setSourceAccountNo(String sourceAccountNo) {
        this.sourceAccountNo = sourceAccountNo;
    }

    public Long getSourceAccountId() {
        return sourceAccountId;
    }

    public void setSourceAccountId(Long sourceAccountId) {
        this.sourceAccountId = sourceAccountId;
    }

    public String getDestinationAccountNo() {
        return destinationAccountNo;
    }

    public void setDestinationAccountNo(String destinationAccountNo) {
        this.destinationAccountNo = destinationAccountNo;
    }

    public Long getDestinationAccountId() {
        return destinationAccountId;
    }

    public void setDestinationAccountId(Long destinationAccountId) {
        this.destinationAccountId = destinationAccountId;
    }

    public Long getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(Long merchantId) {
        this.merchantId = merchantId;
    }

    public Long getCheckoutSessionId() {
        return checkoutSessionId;
    }

    public void setCheckoutSessionId(Long checkoutSessionId) {
        this.checkoutSessionId = checkoutSessionId;
    }

    public String getDeclineReason() {
        return declineReason;
    }

    public void setDeclineReason(String declineReason) {
        this.declineReason = declineReason;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public static class Builder {
        private final TransactionResponse res = new TransactionResponse();

        public Builder transactionId(Long transactionId) {
            res.setTransactionId(transactionId);
            return this;
        }

        public Builder reference(String reference) {
            res.setReference(reference);
            return this;
        }

        public Builder idempotencyKey(String idempotencyKey) {
            res.setIdempotencyKey(idempotencyKey);
            return this;
        }

        public Builder type(TransactionType type) {
            res.setType(type);
            return this;
        }

        public Builder channelCode(String channelCode) {
            res.setChannelCode(channelCode);
            return this;
        }

        public Builder channelId(Long channelId) {
            res.setChannelId(channelId);
            return this;
        }

        public Builder status(TransactionStatus status) {
            res.setStatus(status);
            return this;
        }

        public Builder amount(BigDecimal amount) {
            res.setAmount(amount);
            return this;
        }

        public Builder currency(String currency) {
            res.setCurrency(currency);
            return this;
        }

        public Builder sourceAccountNo(String sourceAccountNo) {
            res.setSourceAccountNo(sourceAccountNo);
            return this;
        }

        public Builder sourceAccountId(Long sourceAccountId) {
            res.setSourceAccountId(sourceAccountId);
            return this;
        }

        public Builder destinationAccountNo(String destinationAccountNo) {
            res.setDestinationAccountNo(destinationAccountNo);
            return this;
        }

        public Builder destinationAccountId(Long destinationAccountId) {
            res.setDestinationAccountId(destinationAccountId);
            return this;
        }

        public Builder merchantId(Long merchantId) {
            res.setMerchantId(merchantId);
            return this;
        }

        public Builder checkoutSessionId(Long checkoutSessionId) {
            res.setCheckoutSessionId(checkoutSessionId);
            return this;
        }

        public Builder declineReason(String declineReason) {
            res.setDeclineReason(declineReason);
            return this;
        }

        public Builder balance(BigDecimal balance) {
            res.setBalance(balance);
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            res.setCreatedAt(createdAt);
            return this;
        }

        public Builder updatedAt(Instant updatedAt) {
            res.setUpdatedAt(updatedAt);
            return this;
        }

        public TransactionResponse build() {
            return res;
        }
    }
}
