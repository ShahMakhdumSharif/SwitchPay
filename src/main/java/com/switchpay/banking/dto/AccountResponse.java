package com.switchpay.banking.dto;

import com.switchpay.banking.entity.Account;
import com.switchpay.banking.enums.AccountStatus;
import com.switchpay.banking.enums.AccountType;
import java.math.BigDecimal;
import java.time.Instant;

public class AccountResponse {

    private Long id;
    private String accountNo;
    private Long userId;
    private AccountType type;
    private BigDecimal balance;
    private AccountStatus status;
    private String currency;
    private Instant createdAt;
    private Instant updatedAt;

    public AccountResponse() {
    }

    public AccountResponse(Long id, String accountNo, Long userId, AccountType type, BigDecimal balance,
                           AccountStatus status, String currency, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.accountNo = accountNo;
        this.userId = userId;
        this.type = type;
        this.balance = balance;
        this.status = status;
        this.currency = currency;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static AccountResponse fromEntity(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getAccountNo(),
                account.getUserId(),
                account.getType(),
                account.getBalance(),
                account.getStatus(),
                account.getCurrency(),
                account.getCreatedAt(),
                account.getUpdatedAt()
        );
    }

    public Long getId() {
        return id;
    }

    public String getAccountNo() {
        return accountNo;
    }

    public Long getUserId() {
        return userId;
    }

    public AccountType getType() {
        return type;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public String getCurrency() {
        return currency;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
