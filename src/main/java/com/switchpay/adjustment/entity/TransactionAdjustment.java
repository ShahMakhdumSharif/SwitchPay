package com.switchpay.adjustment.entity;

import com.switchpay.common.enums.AdjustmentType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "transaction_adjustments")
public class TransactionAdjustment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "original_transaction_id", nullable = false)
    private Long originalTransactionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AdjustmentType type;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(length = 255)
    private String reason;

    @Column(nullable = false, length = 32)
    private String status = "COMPLETED";

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public TransactionAdjustment() {
    }

    public TransactionAdjustment(Long originalTransactionId, AdjustmentType type, BigDecimal amount,
                                 String reason, String status, Long createdBy, Instant createdAt) {
        this.originalTransactionId = originalTransactionId;
        this.type = type;
        this.amount = amount;
        this.reason = reason;
        this.status = status != null ? status : "COMPLETED";
        this.createdBy = createdBy;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOriginalTransactionId() {
        return originalTransactionId;
    }

    public void setOriginalTransactionId(Long originalTransactionId) {
        this.originalTransactionId = originalTransactionId;
    }

    public AdjustmentType getType() {
        return type;
    }

    public void setType(AdjustmentType type) {
        this.type = type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
