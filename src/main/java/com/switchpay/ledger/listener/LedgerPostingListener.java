package com.switchpay.ledger.listener;

import com.switchpay.common.dto.PostingRequest;
import com.switchpay.common.enums.EntryType;
import com.switchpay.common.enums.TransactionStatus;
import com.switchpay.common.enums.TransactionType;
import com.switchpay.common.event.TransactionCompletedEvent;
import com.switchpay.common.port.LedgerPort;
import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class LedgerPostingListener {

    private static final Logger log = LoggerFactory.getLogger(LedgerPostingListener.class);
    private final LedgerPort ledgerPort;

    public LedgerPostingListener(LedgerPort ledgerPort) {
        this.ledgerPort = ledgerPort;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onTransactionCompleted(TransactionCompletedEvent event) {
        if (event == null || event.getStatus() != TransactionStatus.APPROVED) {
            return;
        }

        BigDecimal amount = event.getAmount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        TransactionType type = event.getType();
        if (type == null) {
            return;
        }

        PostingRequest.Builder builder = PostingRequest.builder()
                .transactionId(event.getTransactionId())
                .transactionRef(event.getTransactionRef());

        switch (type) {
            case CASH_WITHDRAWAL -> {
                builder.line(event.getSourceAccountId(), event.getSourceAccountNo(), EntryType.DEBIT, amount);
                builder.line(null, "SYS-CASH", EntryType.CREDIT, amount);
            }
            case TRANSFER -> {
                builder.line(event.getSourceAccountId(), event.getSourceAccountNo(), EntryType.DEBIT, amount);
                builder.line(event.getDestinationAccountId(), event.getDestinationAccountNo(), EntryType.CREDIT, amount);
            }
            case MERCHANT_PAYMENT -> {
                builder.line(event.getSourceAccountId(), event.getSourceAccountNo(), EntryType.DEBIT, amount);
                builder.line(null, "SYS-MERCHANT-PAYABLE", EntryType.CREDIT, amount);
            }
            default -> {
                // Non-financial transactions (ACCOUNT_VALIDATION, BALANCE_INQUIRY, etc.) produce no ledger entries
                return;
            }
        }

        try {
            ledgerPort.post(builder.build());
        } catch (Exception e) {
            log.error("Failed to post ledger entries for transaction {}", event.getTransactionRef(), e);
            throw e;
        }
    }
}
