package com.switchpay.switching.auth;

import com.switchpay.common.dto.TransactionRequest;
import com.switchpay.common.enums.TransactionType;
import com.switchpay.common.port.AccountPort;
import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(3)
public class SufficientBalanceRule implements AuthorizationRule {

    private final AccountPort accountPort;

    public SufficientBalanceRule(AccountPort accountPort) {
        this.accountPort = accountPort;
    }

    @Autowired
    public SufficientBalanceRule(Optional<AccountPort> accountPort) {
        this.accountPort = accountPort.orElse(null);
    }

    @Override
    public AuthorizationDecision evaluate(TransactionRequest request) {
        if (!requiresBalanceCheck(request.getType())) {
            return AuthorizationDecision.approve();
        }
        String sourceAccountNo = request.getSourceAccountNo();
        BigDecimal amount = request.getAmount();
        if (accountPort != null && sourceAccountNo != null && !sourceAccountNo.isBlank() && amount != null) {
            BigDecimal currentBalance = accountPort.balanceOf(sourceAccountNo);
            if (currentBalance == null || currentBalance.compareTo(amount) < 0) {
                return AuthorizationDecision.decline("Insufficient balance");
            }
        }
        return AuthorizationDecision.approve();
    }

    private boolean requiresBalanceCheck(TransactionType type) {
        return type == TransactionType.CASH_WITHDRAWAL
                || type == TransactionType.TRANSFER
                || type == TransactionType.MERCHANT_PAYMENT;
    }
}
