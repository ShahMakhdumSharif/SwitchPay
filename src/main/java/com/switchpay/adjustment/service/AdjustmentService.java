package com.switchpay.adjustment.service;

import com.switchpay.adjustment.dto.AdjustmentResponse;
import com.switchpay.adjustment.dto.RefundRequest;
import com.switchpay.adjustment.dto.ReversalRequest;
import com.switchpay.adjustment.entity.TransactionAdjustment;
import com.switchpay.adjustment.processor.RefundProcessor;
import com.switchpay.adjustment.processor.ReversalProcessor;
import com.switchpay.adjustment.repository.TransactionAdjustmentRepository;
import com.switchpay.common.dto.TransactionResponse;
import com.switchpay.common.exception.NotFoundException;
import com.switchpay.common.port.TransactionQueryPort;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdjustmentService {

    private final RefundProcessor refundProcessor;
    private final ReversalProcessor reversalProcessor;
    private final TransactionAdjustmentRepository adjustmentRepository;
    private final TransactionQueryPort transactionQueryPort;

    public AdjustmentService(
            RefundProcessor refundProcessor,
            ReversalProcessor reversalProcessor,
            TransactionAdjustmentRepository adjustmentRepository,
            TransactionQueryPort transactionQueryPort) {
        this.refundProcessor = refundProcessor;
        this.reversalProcessor = reversalProcessor;
        this.adjustmentRepository = adjustmentRepository;
        this.transactionQueryPort = transactionQueryPort;
    }

    public AdjustmentResponse processRefund(RefundRequest request, Long actorUserId) {
        return refundProcessor.process(request.getTransactionRef(), request.getAmount(), request.getReason(), actorUserId);
    }

    public AdjustmentResponse processReversal(ReversalRequest request, Long actorUserId) {
        return reversalProcessor.process(request.getTransactionRef(), null, request.getReason(), actorUserId);
    }

    @Transactional(readOnly = true)
    public List<AdjustmentResponse> getAdjustments(String transactionRef) {
        if (transactionRef != null && !transactionRef.isBlank()) {
            TransactionResponse txn = transactionQueryPort.findByRef(transactionRef)
                    .orElseThrow(() -> new NotFoundException("Transaction not found: " + transactionRef));
            List<TransactionAdjustment> adjustments = adjustmentRepository.findByOriginalTransactionId(txn.getTransactionId());
            List<AdjustmentResponse> list = new ArrayList<>();
            for (TransactionAdjustment a : adjustments) {
                list.add(AdjustmentResponse.fromEntity(a, txn.getReference()));
            }
            return list;
        }

        List<TransactionAdjustment> all = adjustmentRepository.findAll();
        List<AdjustmentResponse> list = new ArrayList<>();
        for (TransactionAdjustment a : all) {
            String ref = "TXN-" + a.getOriginalTransactionId();
            list.add(AdjustmentResponse.fromEntity(a, ref));
        }
        return list;
    }
}
