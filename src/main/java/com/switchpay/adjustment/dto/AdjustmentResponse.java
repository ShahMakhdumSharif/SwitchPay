package com.switchpay.adjustment.dto;

import com.switchpay.adjustment.entity.TransactionAdjustment;
import com.switchpay.common.enums.AdjustmentType;
import java.math.BigDecimal;
import java.time.Instant;

public class AdjustmentResponse {

    private Long id;
    private Long originalTransactionId;
    private String transactionRef;
    private AdjustmentType type;
    private BigDecimal amount;
    private String reason;
    private String status;
    private Long createdBy;
    private Instant createdAt;

    public AdjustmentResponse() {
    }

    public AdjustmentResponse(Long id, Long originalTransactionId, String transactionRef, AdjustmentType type,
                              BigDecimal amount, String reason, String status, Long createdBy, Instant createdAt) {
        this.id = id;
        this.originalTransactionId = originalTransactionId;
        this.transactionRef = transactionRef;
        this.type = type;
        this.amount = amount;
        this.reason = reason;
        this.status = status;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    public static AdjustmentResponse fromEntity(TransactionAdjustment entity, String transactionRef) {
        return new AdjustmentResponse(
                entity.getId(),
                entity.getOriginalTransactionId(),
                transactionRef,
                entity.getType(),
                entity.getAmount(),
                entity.getReason(),
                entity.getStatus(),
                entity.getCreatedBy(),
                entity.getCreatedAt()
        );
    }

    public Long getId() {
        return id;
    }

    public Long getOriginalTransactionId() {
        return originalTransactionId;
    }

    public String getTransactionRef() {
        return transactionRef;
    }

    public AdjustmentType getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getReason() {
        return reason;
    }

    public String getStatus() {
        return status;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
