package com.switchpay.banking.dto;

import com.switchpay.banking.enums.AccountStatus;
import com.switchpay.banking.enums.AccountType;

public class UpdateAccountRequest {

    private AccountStatus status;
    private AccountType type;

    public UpdateAccountRequest() {
    }

    public UpdateAccountRequest(AccountStatus status, AccountType type) {
        this.status = status;
        this.type = type;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }

    public AccountType getType() {
        return type;
    }

    public void setType(AccountType type) {
        this.type = type;
    }
}
