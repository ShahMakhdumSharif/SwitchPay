package com.switchpay.banking.handler;

import com.switchpay.common.dto.HandlerResult;
import com.switchpay.common.dto.TransactionRequest;
import com.switchpay.common.enums.TransactionType;
import com.switchpay.common.exception.UnprocessableException;
import com.switchpay.common.port.AccountPort;
import com.switchpay.common.port.TransactionHandler;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class BankingTransactionHandler implements TransactionHandler {

    private final AccountPort accountPort;

    public BankingTransactionHandler(AccountPort accountPort) {
        this.accountPort = accountPort;
    }

    @Override
    public boolean supports(TransactionType type) {
        return type == TransactionType.ACCOUNT_VALIDATION
                || type == TransactionType.BALANCE_INQUIRY
                || type == TransactionType.CASH_WITHDRAWAL
                || type == TransactionType.TRANSFER;
    }

    @Override
    @Transactional
    public HandlerResult handle(TransactionRequest request) {
        return handle(request, "TXN-" + System.currentTimeMillis());
    }

    @Override
    @Transactional
    public HandlerResult handle(TransactionRequest request, String transactionRef) {
        if (request == null || request.getType() == null) {
            return HandlerResult.declined("Invalid transaction request");
        }

        return switch (request.getType()) {
            case ACCOUNT_VALIDATION -> handleAccountValidation(request);
            case BALANCE_INQUIRY -> handleBalanceInquiry(request);
            case CASH_WITHDRAWAL -> handleCashWithdrawal(request, transactionRef);
            case TRANSFER -> handleTransfer(request, transactionRef);
            default -> HandlerResult.declined("Unsupported transaction type for banking handler: " + request.getType());
        };
    }

    private HandlerResult handleAccountValidation(TransactionRequest request) {
        String accountNo = request.getSourceAccountNo();
        if (accountNo == null || accountNo.isBlank()) {
            return HandlerResult.declined("Source account number is required");
        }
        try {
            boolean valid = accountPort.validate(accountNo);
            if (!valid) {
                return HandlerResult.declined("Source account is invalid or inactive: " + accountNo);
            }
            return HandlerResult.approved();
        } catch (UnprocessableException ex) {
            return HandlerResult.declined(ex.getMessage());
        } catch (Exception ex) {
            return HandlerResult.declined("Account validation failed: " + ex.getMessage());
        }
    }

    private HandlerResult handleBalanceInquiry(TransactionRequest request) {
        String accountNo = request.getSourceAccountNo();
        if (accountNo == null || accountNo.isBlank()) {
            return HandlerResult.declined("Source account number is required");
        }
        try {
            boolean valid = accountPort.validate(accountNo);
            if (!valid) {
                return HandlerResult.declined("Source account is invalid or inactive: " + accountNo);
            }
            BigDecimal balance = accountPort.balanceOf(accountNo);
            return HandlerResult.approvedWithBalance(balance);
        } catch (UnprocessableException ex) {
            return HandlerResult.declined(ex.getMessage());
        } catch (Exception ex) {
            return HandlerResult.declined("Balance inquiry failed: " + ex.getMessage());
        }
    }

    private HandlerResult handleCashWithdrawal(TransactionRequest request, String transactionRef) {
        String accountNo = request.getSourceAccountNo();
        BigDecimal amount = request.getAmount();

        if (accountNo == null || accountNo.isBlank()) {
            return HandlerResult.declined("Source account number is required");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return HandlerResult.declined("Withdrawal amount must be greater than zero");
        }

        try {
            boolean valid = accountPort.validate(accountNo);
            if (!valid) {
                return HandlerResult.declined("Source account is invalid or inactive: " + accountNo);
            }
            accountPort.debit(accountNo, amount, transactionRef);
            BigDecimal newBalance = accountPort.balanceOf(accountNo);
            return HandlerResult.approvedWithBalance(newBalance);
        } catch (UnprocessableException ex) {
            return HandlerResult.declined(ex.getMessage());
        } catch (Exception ex) {
            return HandlerResult.failed("Withdrawal execution failed: " + ex.getMessage());
        }
    }

    private HandlerResult handleTransfer(TransactionRequest request, String transactionRef) {
        String sourceAccountNo = request.getSourceAccountNo();
        String destinationAccountNo = request.getDestinationAccountNo();
        BigDecimal amount = request.getAmount();

        if (sourceAccountNo == null || sourceAccountNo.isBlank()) {
            return HandlerResult.declined("Source account number is required");
        }
        if (destinationAccountNo == null || destinationAccountNo.isBlank()) {
            return HandlerResult.declined("Destination account number is required");
        }
        if (sourceAccountNo.trim().equalsIgnoreCase(destinationAccountNo.trim())) {
            return HandlerResult.declined("Source and destination accounts must be different");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return HandlerResult.declined("Transfer amount must be greater than zero");
        }

        try {
            boolean sourceValid = accountPort.validate(sourceAccountNo);
            if (!sourceValid) {
                return HandlerResult.declined("Source account is invalid or inactive: " + sourceAccountNo);
            }
            boolean destValid = accountPort.validate(destinationAccountNo);
            if (!destValid) {
                return HandlerResult.declined("Destination account is invalid or inactive: " + destinationAccountNo);
            }

            // Pessimistic locking inside AccountService prevents concurrent overdraws
            accountPort.debit(sourceAccountNo, amount, transactionRef);
            accountPort.credit(destinationAccountNo, amount, transactionRef);

            BigDecimal newBalance = accountPort.balanceOf(sourceAccountNo);
            return HandlerResult.approvedWithBalance(newBalance);
        } catch (UnprocessableException ex) {
            return HandlerResult.declined(ex.getMessage());
        } catch (Exception ex) {
            return HandlerResult.failed("Transfer execution failed: " + ex.getMessage());
        }
    }
}
