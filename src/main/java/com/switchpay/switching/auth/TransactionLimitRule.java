package com.switchpay.switching.auth;

import com.switchpay.common.dto.TransactionRequest;
import com.switchpay.common.enums.TransactionType;
import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(2)
public class TransactionLimitRule implements AuthorizationRule {

    private final BigDecimal maxTransactionLimit;

    public TransactionLimitRule(
            @Value("${switchpay.switch.transaction-limit:100000.00}") BigDecimal maxTransactionLimit) {
        this.maxTransactionLimit = maxTransactionLimit;
    }

    @Override
    public AuthorizationDecision evaluate(TransactionRequest request) {
        if (isMonetaryTransaction(request.getType()) && request.getAmount() != null) {
            if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                return AuthorizationDecision.decline("Transaction amount must be positive");
            }
            if (request.getAmount().compareTo(maxTransactionLimit) > 0) {
                return AuthorizationDecision.decline("Transaction amount exceeds per-transaction limit");
            }
        }
        return AuthorizationDecision.approve();
    }

    private boolean isMonetaryTransaction(TransactionType type) {
        return type == TransactionType.CASH_WITHDRAWAL
                || type == TransactionType.TRANSFER
                || type == TransactionType.MERCHANT_PAYMENT;
    }
}
