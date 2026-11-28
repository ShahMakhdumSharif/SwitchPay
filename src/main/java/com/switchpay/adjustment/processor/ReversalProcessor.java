package com.switchpay.adjustment.processor;

import com.switchpay.adjustment.entity.TransactionAdjustment;
import com.switchpay.adjustment.repository.TransactionAdjustmentRepository;
import com.switchpay.common.dto.TransactionResponse;
import com.switchpay.common.enums.AdjustmentType;
import com.switchpay.common.enums.TransactionStatus;
import com.switchpay.common.exception.ConflictException;
import com.switchpay.common.port.AuditPort;
import com.switchpay.common.port.LedgerPort;
import com.switchpay.common.port.SettlementPort;
import com.switchpay.common.port.TransactionQueryPort;
import java.math.BigDecimal;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ReversalProcessor extends AbstractAdjustmentProcessor {

    public ReversalProcessor(
            TransactionAdjustmentRepository adjustmentRepository,
            TransactionQueryPort transactionQueryPort,
            LedgerPort ledgerPort,
            @Autowired(required = false) SettlementPort settlementPort,
            AuditPort auditPort,
            Clock clock) {
        super(adjustmentRepository, transactionQueryPort, ledgerPort, settlementPort, auditPort, clock);
    }

    @Override
    public AdjustmentType getAdjustmentType() {
        return AdjustmentType.REVERSAL;
    }

    @Override
    protected BigDecimal validate(TransactionResponse originalTxn, BigDecimal requestedAmount, String reason) {
        if (originalTxn.getStatus() == TransactionStatus.REVERSED ||
                adjustmentRepository.existsByOriginalTransactionIdAndTypeAndStatus(
                        originalTxn.getTransactionId(), AdjustmentType.REVERSAL, "COMPLETED")) {
            throw new ConflictException("Transaction " + originalTxn.getReference() + " has already been reversed");
        }

        if (originalTxn.getStatus() == TransactionStatus.SETTLED) {
            throw new ConflictException("Cannot reverse an already settled transaction: " + originalTxn.getReference());
        }

        if (originalTxn.getStatus() != TransactionStatus.APPROVED) {
            throw new ConflictException("Only APPROVED transactions can be reversed. Current status: " + originalTxn.getStatus());
        }

        return originalTxn.getAmount();
    }

    @Override
    protected void postLedger(TransactionResponse originalTxn, TransactionAdjustment adjustment) {
        ledgerPort.reverse(originalTxn.getReference(), "ADJ-" + adjustment.getId());
    }

    @Override
    protected void updateTransactionStatus(TransactionResponse originalTxn, TransactionAdjustment adjustment) {
        transactionQueryPort.markStatus(originalTxn.getReference(), TransactionStatus.REVERSED);
    }
}
