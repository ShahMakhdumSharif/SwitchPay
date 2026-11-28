package com.switchpay.adjustment.processor;

import com.switchpay.adjustment.dto.AdjustmentResponse;
import com.switchpay.adjustment.entity.TransactionAdjustment;
import com.switchpay.adjustment.repository.TransactionAdjustmentRepository;
import com.switchpay.common.dto.TransactionResponse;
import com.switchpay.common.enums.AdjustmentType;
import com.switchpay.common.exception.NotFoundException;
import com.switchpay.common.port.AuditPort;
import com.switchpay.common.port.LedgerPort;
import com.switchpay.common.port.SettlementPort;
import com.switchpay.common.port.TransactionQueryPort;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import org.springframework.transaction.annotation.Transactional;

/**
 * Template Method Pattern for processing transaction adjustments.
 * Defines the skeleton of adjustment processing:
 * 1. Fetch transaction
 * 2. Validate adjustment rules (Template Hook)
 * 3. Create adjustment record
 * 4. Post double-entry ledger entries (Template Hook)
 * 5. Update transaction status (Template Hook)
 * 6. Adjust settlement if applicable (Template Hook)
 * 7. Record audit trail
 */
public abstract class AbstractAdjustmentProcessor {

    protected final TransactionAdjustmentRepository adjustmentRepository;
    protected final TransactionQueryPort transactionQueryPort;
    protected final LedgerPort ledgerPort;
    protected final SettlementPort settlementPort;
    protected final AuditPort auditPort;
    protected final Clock clock;

    public AbstractAdjustmentProcessor(
            TransactionAdjustmentRepository adjustmentRepository,
            TransactionQueryPort transactionQueryPort,
            LedgerPort ledgerPort,
            SettlementPort settlementPort,
            AuditPort auditPort,
            Clock clock) {
        this.adjustmentRepository = adjustmentRepository;
        this.transactionQueryPort = transactionQueryPort;
        this.ledgerPort = ledgerPort;
        this.settlementPort = settlementPort;
        this.auditPort = auditPort;
        this.clock = clock;
    }

    public abstract AdjustmentType getAdjustmentType();

    @Transactional
    public AdjustmentResponse process(String txnRef, BigDecimal amount, String reason, Long actorUserId) {
        TransactionResponse originalTxn = transactionQueryPort.findByRef(txnRef)
                .orElseThrow(() -> new NotFoundException("Transaction not found: " + txnRef));

        BigDecimal effectiveAmount = validate(originalTxn, amount, reason);

        TransactionAdjustment adjustment = createAdjustmentRecord(originalTxn, effectiveAmount, reason, actorUserId);

        postLedger(originalTxn, adjustment);

        updateTransactionStatus(originalTxn, adjustment);

        adjustSettlement(originalTxn, adjustment);

        audit(originalTxn, adjustment, actorUserId);

        return AdjustmentResponse.fromEntity(adjustment, originalTxn.getReference());
    }

    protected TransactionAdjustment createAdjustmentRecord(
            TransactionResponse originalTxn, BigDecimal amount, String reason, Long actorUserId) {
        Instant now = Instant.now(clock);
        TransactionAdjustment adjustment = new TransactionAdjustment(
                originalTxn.getTransactionId(),
                getAdjustmentType(),
                amount,
                reason,
                "COMPLETED",
                actorUserId,
                now
        );
        return adjustmentRepository.save(adjustment);
    }

    protected abstract BigDecimal validate(TransactionResponse originalTxn, BigDecimal requestedAmount, String reason);

    protected abstract void postLedger(TransactionResponse originalTxn, TransactionAdjustment adjustment);

    protected abstract void updateTransactionStatus(TransactionResponse originalTxn, TransactionAdjustment adjustment);

    protected void adjustSettlement(TransactionResponse originalTxn, TransactionAdjustment adjustment) {
        // Default no-op, overridden by processors requiring settlement adjustment (e.g. RefundProcessor)
    }

    protected void audit(TransactionResponse originalTxn, TransactionAdjustment adjustment, Long actorUserId) {
        String details = String.format("Type: %s, Amount: %s, Reason: %s",
                adjustment.getType(), adjustment.getAmount(), adjustment.getReason());
        auditPort.record(actorUserId, "ADJUSTMENT_" + adjustment.getType(), originalTxn.getReference(), details);
    }
}
