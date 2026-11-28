package com.switchpay.gateway.handler;

import com.switchpay.common.dto.HandlerResult;
import com.switchpay.common.dto.TransactionRequest;
import com.switchpay.common.enums.TransactionType;
import com.switchpay.common.port.AccountPort;
import com.switchpay.common.port.TransactionHandler;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/**
 * Strategy for MERCHANT_PAYMENT, picked by the switch's TransactionRouter.
 * The switch already checked account status, limit and balance (authorization chain),
 * so here we only move the money: buyer account -> merchant payable account.
 *
 * Day 3: link the payment to its checkout session and mark the session PAID.
 */
@Component
public class PaymentTransactionHandler implements TransactionHandler {

    private static final String MERCHANT_PAYABLE_ACCOUNT = "SYS-MERCHANT-PAYABLE";

    private final AccountPort accountPort;

    public PaymentTransactionHandler(AccountPort accountPort) {
        this.accountPort = accountPort;
    }

    @Override
    public boolean supports(TransactionType type) {
        return type == TransactionType.MERCHANT_PAYMENT;
    }

    @Override
    public HandlerResult handle(TransactionRequest request) {
        return handle(request, null);
    }

    @Override
    public HandlerResult handle(TransactionRequest request, String transactionRef) {
        BigDecimal amount = request.getAmount();
        if (amount == null || amount.signum() <= 0) {
            return HandlerResult.declined("Payment amount must be greater than zero");
        }

        String destination = request.getDestinationAccountNo() != null
                ? request.getDestinationAccountNo()
                : MERCHANT_PAYABLE_ACCOUNT;

        accountPort.debit(request.getSourceAccountNo(), amount, transactionRef);
        accountPort.credit(destination, amount, transactionRef);

        return HandlerResult.approved();
    }
}
