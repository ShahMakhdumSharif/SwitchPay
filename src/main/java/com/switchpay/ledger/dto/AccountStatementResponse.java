package com.switchpay.ledger.dto;

import java.math.BigDecimal;
import java.util.List;

public class AccountStatementResponse {

    private String accountNo;
    private Long accountId;
    private BigDecimal totalDebits;
    private BigDecimal totalCredits;
    private BigDecimal netChange;
    private List<LedgerEntryResponse> entries;

    public AccountStatementResponse() {
    }

    public AccountStatementResponse(String accountNo, Long accountId, BigDecimal totalDebits,
                                    BigDecimal totalCredits, BigDecimal netChange, List<LedgerEntryResponse> entries) {
        this.accountNo = accountNo;
        this.accountId = accountId;
        this.totalDebits = totalDebits;
        this.totalCredits = totalCredits;
        this.netChange = netChange;
        this.entries = entries;
    }

    public String getAccountNo() {
        return accountNo;
    }

    public Long getAccountId() {
        return accountId;
    }

    public BigDecimal getTotalDebits() {
        return totalDebits;
    }

    public BigDecimal getTotalCredits() {
        return totalCredits;
    }

    public BigDecimal getNetChange() {
        return netChange;
    }

    public List<LedgerEntryResponse> getEntries() {
        return entries;
    }
}
