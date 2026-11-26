package com.switchpay.adjustment.processor;

import com.switchpay.adjustment.entity.TransactionAdjustment;
import com.switchpay.adjustment.repository.TransactionAdjustmentRepository;
import com.switchpay.common.dto.PostingRequest;
import com.switchpay.common.dto.TransactionResponse;
import com.switchpay.common.enums.AdjustmentType;
import com.switchpay.common.enums.EntryType;
import com.switchpay.common.enums.TransactionStatus;
import com.switchpay.common.enums.TransactionType;
import com.switchpay.common.exception.BadRequestException;
import com.switchpay.common.exception.ConflictException;
import com.switchpay.common.exception.UnprocessableException;
import com.switchpay.common.port.AuditPort;
import com.switchpay.common.port.LedgerPort;
import com.switchpay.common.port.SettlementPort;
import com.switchpay.common.port.TransactionQueryPort;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RefundProcessor extends AbstractAdjustmentProcessor {

    public RefundProcessor(
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
        return AdjustmentType.REFUND;
    }

    @Override
    protected BigDecimal validate(TransactionResponse originalTxn, BigDecimal requestedAmount, String reason) {
        if (requestedAmount == null || requestedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Refund amount must be positive");
        }

        if (originalTxn.getType() != TransactionType.MERCHANT_PAYMENT) {
            throw new ConflictException("Refunds are only allowed for MERCHANT_PAYMENT transactions");
        }

        if (originalTxn.getStatus() != TransactionStatus.APPROVED && originalTxn.getStatus() != TransactionStatus.SETTLED) {
            throw new ConflictException("Refunds only allowed on APPROVED or SETTLED transactions. Current status: " + originalTxn.getStatus());
        }

        List<TransactionAdjustment> existingAdjustments = adjustmentRepository.findByOriginalTransactionIdAndType(
                originalTxn.getTransactionId(), AdjustmentType.REFUND);

        BigDecimal alreadyRefunded = BigDecimal.ZERO;
        for (TransactionAdjustment existing : existingAdjustments) {
            if ("COMPLETED".equalsIgnoreCase(existing.getStatus())) {
                alreadyRefunded = alreadyRefunded.add(existing.getAmount());
            }
        }

        BigDecimal remaining = originalTxn.getAmount().subtract(alreadyRefunded);
        if (requestedAmount.compareTo(remaining) > 0) {
            throw new UnprocessableException(
                    "Refund amount (" + requestedAmount + ") exceeds remaining refundable amount (" + remaining + ")");
        }

        return requestedAmount;
    }

    @Override
    protected void postLedger(TransactionResponse originalTxn, TransactionAdjustment adjustment) {
        PostingRequest postingRequest = PostingRequest.builder()
                .transactionId(originalTxn.getTransactionId())
                .transactionRef(originalTxn.getReference())
                .adjustmentId(adjustment.getId())
                .line(null, "SYS-MERCHANT-PAYABLE", EntryType.DEBIT, adjustment.getAmount())
                .line(originalTxn.getSourceAccountId(), originalTxn.getSourceAccountNo(), EntryType.CREDIT, adjustment.getAmount())
                .build();
        ledgerPort.post(postingRequest);
    }

    @Override
    protected void updateTransactionStatus(TransactionResponse originalTxn, TransactionAdjustment adjustment) {
        List<TransactionAdjustment> existingAdjustments = adjustmentRepository.findByOriginalTransactionIdAndType(
                originalTxn.getTransactionId(), AdjustmentType.REFUND);

        BigDecimal totalRefunded = adjustment.getAmount();
        for (TransactionAdjustment existing : existingAdjustments) {
            if ("COMPLETED".equalsIgnoreCase(existing.getStatus()) && !existing.getId().equals(adjustment.getId())) {
                totalRefunded = totalRefunded.add(existing.getAmount());
            }
        }

        if (totalRefunded.compareTo(originalTxn.getAmount()) >= 0) {
            transactionQueryPort.markStatus(originalTxn.getReference(), TransactionStatus.REFUNDED);
        }
    }

    @Override
    protected void adjustSettlement(TransactionResponse originalTxn, TransactionAdjustment adjustment) {
        if (settlementPort != null && originalTxn.getMerchantId() != null) {
            settlementPort.adjustForRefund(originalTxn.getMerchantId(), originalTxn.getReference(), adjustment.getAmount());
        }
    }
}
