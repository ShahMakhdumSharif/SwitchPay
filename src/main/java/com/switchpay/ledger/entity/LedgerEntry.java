package com.switchpay.ledger.entity;

import com.switchpay.common.enums.EntryType;
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
@Table(name = "ledger_entries")
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id", nullable = false)
    private Long transactionId;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, length = 32)
    private EntryType entryType;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "adjustment_id")
    private Long adjustmentId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public LedgerEntry() {
    }

    private LedgerEntry(Builder builder) {
        this.id = builder.id;
        this.transactionId = builder.transactionId;
        this.accountId = builder.accountId;
        this.entryType = builder.entryType;
        this.amount = builder.amount;
        this.adjustmentId = builder.adjustmentId;
        this.createdAt = builder.createdAt != null ? builder.createdAt : Instant.now();
    }

    public static Builder builder() {
        return new Builder();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTransactionId() {
        return transactionId;
    }

    public Long getAccountId() {
        return accountId;
    }

    public EntryType getEntryType() {
        return entryType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Long getAdjustmentId() {
        return adjustmentId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public static class Builder {
        private Long id;
        private Long transactionId;
        private Long accountId;
        private EntryType entryType;
        private BigDecimal amount;
        private Long adjustmentId;
        private Instant createdAt;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder transactionId(Long transactionId) {
            this.transactionId = transactionId;
            return this;
        }

        public Builder accountId(Long accountId) {
            this.accountId = accountId;
            return this;
        }

        public Builder entryType(EntryType entryType) {
            this.entryType = entryType;
            return this;
        }

        public Builder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public Builder adjustmentId(Long adjustmentId) {
            this.adjustmentId = adjustmentId;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public LedgerEntry build() {
            return new LedgerEntry(this);
        }
    }
}
