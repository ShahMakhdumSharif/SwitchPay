package com.switchpay.common.dto;

import com.switchpay.common.enums.TransactionType;
import java.math.BigDecimal;

public class TransactionRequest {

    private String idempotencyKey;
    private TransactionType type;
    private String channelCode;
    private Long channelId;
    private BigDecimal amount;
    private String currency = "BDT";
    private String sourceAccountNo;
    private Long sourceAccountId;
    private String destinationAccountNo;
    private Long destinationAccountId;
    private Long merchantId;
    private Long checkoutSessionId;
    private String checkoutSessionToken;
    private Long actorUserId;

    public TransactionRequest() {
    }

    public static Builder builder() {
        return new Builder();
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

    public String getCheckoutSessionToken() {
        return checkoutSessionToken;
    }

    public void setCheckoutSessionToken(String checkoutSessionToken) {
        this.checkoutSessionToken = checkoutSessionToken;
    }

    public Long getActorUserId() {
        return actorUserId;
    }

    public void setActorUserId(Long actorUserId) {
        this.actorUserId = actorUserId;
    }

    public static class Builder {
        private final TransactionRequest req = new TransactionRequest();

        public Builder idempotencyKey(String idempotencyKey) {
            req.setIdempotencyKey(idempotencyKey);
            return this;
        }

        public Builder type(TransactionType type) {
            req.setType(type);
            return this;
        }

        public Builder channelCode(String channelCode) {
            req.setChannelCode(channelCode);
            return this;
        }

        public Builder channelId(Long channelId) {
            req.setChannelId(channelId);
            return this;
        }

        public Builder amount(BigDecimal amount) {
            req.setAmount(amount);
            return this;
        }

        public Builder currency(String currency) {
            req.setCurrency(currency);
            return this;
        }

        public Builder sourceAccountNo(String sourceAccountNo) {
            req.setSourceAccountNo(sourceAccountNo);
            return this;
        }

        public Builder sourceAccountId(Long sourceAccountId) {
            req.setSourceAccountId(sourceAccountId);
            return this;
        }

        public Builder destinationAccountNo(String destinationAccountNo) {
            req.setDestinationAccountNo(destinationAccountNo);
            return this;
        }

        public Builder destinationAccountId(Long destinationAccountId) {
            req.setDestinationAccountId(destinationAccountId);
            return this;
        }

        public Builder merchantId(Long merchantId) {
            req.setMerchantId(merchantId);
            return this;
        }

        public Builder checkoutSessionId(Long checkoutSessionId) {
            req.setCheckoutSessionId(checkoutSessionId);
            return this;
        }

        public Builder checkoutSessionToken(String checkoutSessionToken) {
            req.setCheckoutSessionToken(checkoutSessionToken);
            return this;
        }

        public Builder actorUserId(Long actorUserId) {
            req.setActorUserId(actorUserId);
            return this;
        }

        public TransactionRequest build() {
            return req;
        }
    }
}
