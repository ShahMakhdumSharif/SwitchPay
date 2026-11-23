package com.switchpay.banking.dto;

import com.switchpay.banking.enums.AccountStatus;
import java.math.BigDecimal;

public class BalanceResponse {

    private String accountNo;
    private BigDecimal balance;
    private String currency;
    private AccountStatus status;

    public BalanceResponse() {
    }

    public BalanceResponse(String accountNo, BigDecimal balance, String currency, AccountStatus status) {
        this.accountNo = accountNo;
        this.balance = balance;
        this.currency = currency;
        this.status = status;
    }

    public String getAccountNo() {
        return accountNo;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public String getCurrency() {
        return currency;
    }

    public AccountStatus getStatus() {
        return status;
    }
}
