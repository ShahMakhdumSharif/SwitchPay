package com.switchpay.adjustment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.switchpay.adjustment.dto.AdjustmentResponse;
import com.switchpay.adjustment.dto.RefundRequest;
import com.switchpay.adjustment.dto.ReversalRequest;
import com.switchpay.adjustment.entity.TransactionAdjustment;
import com.switchpay.adjustment.processor.RefundProcessor;
import com.switchpay.adjustment.processor.ReversalProcessor;
import com.switchpay.adjustment.repository.TransactionAdjustmentRepository;
import com.switchpay.common.dto.PostingRequest;
import com.switchpay.common.dto.TransactionResponse;
import com.switchpay.common.enums.AdjustmentType;
import com.switchpay.common.enums.TransactionStatus;
import com.switchpay.common.enums.TransactionType;
import com.switchpay.common.exception.ConflictException;
import com.switchpay.common.exception.UnprocessableException;
import com.switchpay.common.port.AuditPort;
import com.switchpay.common.port.LedgerPort;
import com.switchpay.common.port.SettlementPort;
import com.switchpay.common.port.TransactionQueryPort;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdjustmentServiceTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-11-23T14:00:00Z");

    @Mock
    private TransactionAdjustmentRepository adjustmentRepository;

    @Mock
    private TransactionQueryPort transactionQueryPort;

    @Mock
    private LedgerPort ledgerPort;

    @Mock
    private SettlementPort settlementPort;

    @Mock
    private AuditPort auditPort;

    private Clock clock;
    private RefundProcessor refundProcessor;
    private ReversalProcessor reversalProcessor;
    private AdjustmentService adjustmentService;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
        refundProcessor = new RefundProcessor(adjustmentRepository, transactionQueryPort, ledgerPort, settlementPort, auditPort, clock);
        reversalProcessor = new ReversalProcessor(adjustmentRepository, transactionQueryPort, ledgerPort, settlementPort, auditPort, clock);
        adjustmentService = new AdjustmentService(refundProcessor, reversalProcessor, adjustmentRepository, transactionQueryPort);
    }

    @Test
    @DisplayName("U7: refund_exceedsRemaining_rejected")
    void refund_exceedsRemaining_rejected() {
        String txnRef = "TXN-MP-100";
        TransactionResponse txn = TransactionResponse.builder()
                .transactionId(100L)
                .reference(txnRef)
                .type(TransactionType.MERCHANT_PAYMENT)
                .status(TransactionStatus.APPROVED)
                .amount(new BigDecimal("1000.00"))
                .currency("BDT")
                .sourceAccountId(10L)
                .sourceAccountNo("ACC-BUYER")
                .merchantId(55L)
                .createdAt(FIXED_NOW)
                .build();

        when(transactionQueryPort.findByRef(txnRef)).thenReturn(Optional.of(txn));

        // Existing refund of 600.00
        TransactionAdjustment existingRefund = new TransactionAdjustment(
                100L, AdjustmentType.REFUND, new BigDecimal("600.00"), "First refund", "COMPLETED", 1L, FIXED_NOW
        );
        existingRefund.setId(1L);

        when(adjustmentRepository.findByOriginalTransactionIdAndType(100L, AdjustmentType.REFUND))
                .thenReturn(List.of(existingRefund));

        // Request refund of 500.00 (remaining is 400.00) -> should throw UnprocessableException
        RefundRequest request = new RefundRequest(txnRef, new BigDecimal("500.00"), "Second refund");

        assertThatThrownBy(() -> adjustmentService.processRefund(request, 1L))
                .isInstanceOf(UnprocessableException.class)
                .hasMessageContaining("exceeds remaining refundable amount");

        verify(ledgerPort, never()).post(any());
        verify(transactionQueryPort, never()).markStatus(any(), any());
    }

    @Test
    @DisplayName("U8: reversal_secondAttempt_rejected")
    void reversal_secondAttempt_rejected() {
        String txnRef = "TXN-REV-200";
        TransactionResponse txn = TransactionResponse.builder()
                .transactionId(200L)
                .reference(txnRef)
                .type(TransactionType.TRANSFER)
                .status(TransactionStatus.REVERSED)
                .amount(new BigDecimal("750.00"))
                .currency("BDT")
                .sourceAccountId(20L)
                .sourceAccountNo("ACC-SRC")
                .destinationAccountId(21L)
                .destinationAccountNo("ACC-DST")
                .createdAt(FIXED_NOW)
                .build();

        when(transactionQueryPort.findByRef(txnRef)).thenReturn(Optional.of(txn));

        ReversalRequest request = new ReversalRequest(txnRef, "Duplicate reversal");

        assertThatThrownBy(() -> adjustmentService.processReversal(request, 1L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("has already been reversed");

        verify(ledgerPort, never()).reverse(any(), any());
    }

    @Test
    @DisplayName("Reversal on settled transaction throws ConflictException")
    void reversal_onSettledTransaction_throwsConflict() {
        String txnRef = "TXN-SETTLED-300";
        TransactionResponse txn = TransactionResponse.builder()
                .transactionId(300L)
                .reference(txnRef)
                .type(TransactionType.TRANSFER)
                .status(TransactionStatus.SETTLED)
                .amount(new BigDecimal("500.00"))
                .currency("BDT")
                .createdAt(FIXED_NOW)
                .build();

        when(transactionQueryPort.findByRef(txnRef)).thenReturn(Optional.of(txn));

        ReversalRequest request = new ReversalRequest(txnRef, "Reversal on settled");

        assertThatThrownBy(() -> adjustmentService.processReversal(request, 1L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Cannot reverse an already settled transaction");
    }

    @Test
    @DisplayName("Successful reversal executes template steps: save, reverse ledger, mark status, and audit")
    void reversal_successful_executesAllSteps() {
        String txnRef = "TXN-OK-400";
        TransactionResponse txn = TransactionResponse.builder()
                .transactionId(400L)
                .reference(txnRef)
                .type(TransactionType.TRANSFER)
                .status(TransactionStatus.APPROVED)
                .amount(new BigDecimal("1200.00"))
                .currency("BDT")
                .sourceAccountId(30L)
                .destinationAccountId(31L)
                .createdAt(FIXED_NOW)
                .build();

        when(transactionQueryPort.findByRef(txnRef)).thenReturn(Optional.of(txn));
        when(adjustmentRepository.existsByOriginalTransactionIdAndTypeAndStatus(400L, AdjustmentType.REVERSAL, "COMPLETED"))
                .thenReturn(false);

        when(adjustmentRepository.save(any(TransactionAdjustment.class))).thenAnswer(invocation -> {
            TransactionAdjustment saved = invocation.getArgument(0);
            saved.setId(10L);
            return saved;
        });

        ReversalRequest request = new ReversalRequest(txnRef, "Customer dispute resolved");
        AdjustmentResponse response = adjustmentService.processReversal(request, 2L);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getType()).isEqualTo(AdjustmentType.REVERSAL);
        assertThat(response.getAmount()).isEqualByComparingTo("1200.00");
        assertThat(response.getStatus()).isEqualTo("COMPLETED");

        verify(ledgerPort).reverse(eq(txnRef), eq("ADJ-10"));
        verify(transactionQueryPort).markStatus(txnRef, TransactionStatus.REVERSED);
        verify(auditPort).record(eq(2L), eq("ADJUSTMENT_REVERSAL"), eq(txnRef), any());
    }

    @Test
    @DisplayName("Successful partial and full refund updates ledger, settlement, and transaction status")
    void refund_fullAmount_marksRefundedAndAdjustsSettlement() {
        String txnRef = "TXN-MP-500";
        TransactionResponse txn = TransactionResponse.builder()
                .transactionId(500L)
                .reference(txnRef)
                .type(TransactionType.MERCHANT_PAYMENT)
                .status(TransactionStatus.APPROVED)
                .amount(new BigDecimal("800.00"))
                .currency("BDT")
                .sourceAccountId(40L)
                .sourceAccountNo("ACC-BUYER")
                .merchantId(77L)
                .createdAt(FIXED_NOW)
                .build();

        when(transactionQueryPort.findByRef(txnRef)).thenReturn(Optional.of(txn));
        when(adjustmentRepository.findByOriginalTransactionIdAndType(500L, AdjustmentType.REFUND))
                .thenReturn(List.of());

        when(adjustmentRepository.save(any(TransactionAdjustment.class))).thenAnswer(invocation -> {
            TransactionAdjustment saved = invocation.getArgument(0);
            saved.setId(20L);
            return saved;
        });

        RefundRequest request = new RefundRequest(txnRef, new BigDecimal("800.00"), "Defective product");
        AdjustmentResponse response = adjustmentService.processRefund(request, 5L);

        assertThat(response.getId()).isEqualTo(20L);
        assertThat(response.getType()).isEqualTo(AdjustmentType.REFUND);
        assertThat(response.getAmount()).isEqualByComparingTo("800.00");

        verify(ledgerPort).post(any(PostingRequest.class));
        verify(settlementPort).adjustForRefund(77L, txnRef, new BigDecimal("800.00"));
        verify(transactionQueryPort).markStatus(txnRef, TransactionStatus.REFUNDED);
        verify(auditPort).record(eq(5L), eq("ADJUSTMENT_REFUND"), eq(txnRef), any());
    }
}
