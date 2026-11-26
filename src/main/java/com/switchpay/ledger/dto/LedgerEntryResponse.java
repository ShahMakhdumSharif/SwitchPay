package com.switchpay.ledger.dto;

import com.switchpay.common.enums.EntryType;
import com.switchpay.ledger.entity.LedgerEntry;
import java.math.BigDecimal;
import java.time.Instant;

public class LedgerEntryResponse {

    private Long id;
    private Long transactionId;
    private Long accountId;
    private String accountNo;
    private EntryType entryType;
    private BigDecimal amount;
    private Long adjustmentId;
    private Instant createdAt;

    public LedgerEntryResponse() {
    }

    public LedgerEntryResponse(Long id, Long transactionId, Long accountId, String accountNo,
                               EntryType entryType, BigDecimal amount, Long adjustmentId, Instant createdAt) {
        this.id = id;
        this.transactionId = transactionId;
        this.accountId = accountId;
        this.accountNo = accountNo;
        this.entryType = entryType;
        this.amount = amount;
        this.adjustmentId = adjustmentId;
        this.createdAt = createdAt;
    }

    public static LedgerEntryResponse fromEntity(LedgerEntry entry, String accountNo) {
        return new LedgerEntryResponse(
                entry.getId(),
                entry.getTransactionId(),
                entry.getAccountId(),
                accountNo,
                entry.getEntryType(),
                entry.getAmount(),
                entry.getAdjustmentId(),
                entry.getCreatedAt()
        );
    }

    public Long getId() {
        return id;
    }

    public Long getTransactionId() {
        return transactionId;
    }

    public Long getAccountId() {
        return accountId;
    }

    public String getAccountNo() {
        return accountNo;
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
}
