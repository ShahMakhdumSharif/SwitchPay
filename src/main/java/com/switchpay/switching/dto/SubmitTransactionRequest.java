package com.switchpay.switching.dto;

import com.switchpay.common.enums.TransactionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class SubmitTransactionRequest {

    @NotBlank(message = "channelCode is required")
    private String channelCode;

    @NotNull(message = "type is required")
    private TransactionType type;

    private String sourceAccountNo;

    private String destinationAccountNo;

    private BigDecimal amount;

    @NotBlank(message = "idempotencyKey is required")
    private String idempotencyKey;

    private Long merchantId;

    private Long checkoutSessionId;

    public SubmitTransactionRequest() {
    }

    public String getChannelCode() {
        return channelCode;
    }

    public void setChannelCode(String channelCode) {
        this.channelCode = channelCode;
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public String getSourceAccountNo() {
        return sourceAccountNo;
    }

    public void setSourceAccountNo(String sourceAccountNo) {
        this.sourceAccountNo = sourceAccountNo;
    }

    public String getDestinationAccountNo() {
        return destinationAccountNo;
    }

    public void setDestinationAccountNo(String destinationAccountNo) {
        this.destinationAccountNo = destinationAccountNo;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
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
}
