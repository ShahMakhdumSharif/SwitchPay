package com.switchpay.ledger.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.switchpay.common.dto.PostingRequest;
import com.switchpay.common.enums.EntryType;
import com.switchpay.common.enums.TransactionStatus;
import com.switchpay.common.enums.TransactionType;
import com.switchpay.common.event.TransactionCompletedEvent;
import com.switchpay.common.port.LedgerPort;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LedgerPostingListenerTest {

    @Mock
    private LedgerPort ledgerPort;

    private LedgerPostingListener listener;

    @BeforeEach
    void setUp() {
        listener = new LedgerPostingListener(ledgerPort);
    }

    @Test
    @DisplayName("CASH_WITHDRAWAL approved posts customer debit and SYS-CASH credit")
    void onTransactionCompleted_cashWithdrawal_postsBalancedEntries() {
        TransactionCompletedEvent event = new TransactionCompletedEvent(
                10L, "TXN-CW-1", TransactionType.CASH_WITHDRAWAL, "ATM",
                TransactionStatus.APPROVED, new BigDecimal("500.00"), "BDT",
                "ACC-101", 1L, null, null, null, null, Instant.now()
        );

        listener.onTransactionCompleted(event);

        ArgumentCaptor<PostingRequest> captor = ArgumentCaptor.forClass(PostingRequest.class);
        verify(ledgerPort).post(captor.capture());

        PostingRequest req = captor.getValue();
        assertThat(req.getTransactionId()).isEqualTo(10L);
        assertThat(req.getTransactionRef()).isEqualTo("TXN-CW-1");
        assertThat(req.getLines()).hasSize(2);

        PostingRequest.PostingLine line1 = req.getLines().get(0);
        assertThat(line1.getAccountId()).isEqualTo(1L);
        assertThat(line1.getEntryType()).isEqualTo(EntryType.DEBIT);
        assertThat(line1.getAmount()).isEqualByComparingTo("500.00");

        PostingRequest.PostingLine line2 = req.getLines().get(1);
        assertThat(line2.getAccountNo()).isEqualTo("SYS-CASH");
        assertThat(line2.getEntryType()).isEqualTo(EntryType.CREDIT);
        assertThat(line2.getAmount()).isEqualByComparingTo("500.00");
    }

    @Test
    @DisplayName("TRANSFER approved posts source debit and destination credit")
    void onTransactionCompleted_transfer_postsBalancedEntries() {
        TransactionCompletedEvent event = new TransactionCompletedEvent(
                20L, "TXN-TR-1", TransactionType.TRANSFER, "MOBILE",
                TransactionStatus.APPROVED, new BigDecimal("1200.00"), "BDT",
                "ACC-SRC", 5L, "ACC-DST", 6L, null, null, Instant.now()
        );

        listener.onTransactionCompleted(event);

        ArgumentCaptor<PostingRequest> captor = ArgumentCaptor.forClass(PostingRequest.class);
        verify(ledgerPort).post(captor.capture());

        PostingRequest req = captor.getValue();
        assertThat(req.getLines()).hasSize(2);
        assertThat(req.getLines().get(0).getAccountId()).isEqualTo(5L);
        assertThat(req.getLines().get(0).getEntryType()).isEqualTo(EntryType.DEBIT);
        assertThat(req.getLines().get(1).getAccountId()).isEqualTo(6L);
        assertThat(req.getLines().get(1).getEntryType()).isEqualTo(EntryType.CREDIT);
    }

    @Test
    @DisplayName("MERCHANT_PAYMENT approved posts customer debit and SYS-MERCHANT-PAYABLE credit")
    void onTransactionCompleted_merchantPayment_postsBalancedEntries() {
        TransactionCompletedEvent event = new TransactionCompletedEvent(
                30L, "TXN-MP-1", TransactionType.MERCHANT_PAYMENT, "WEB",
                TransactionStatus.APPROVED, new BigDecimal("3500.00"), "BDT",
                "ACC-BUYER", 8L, "SYS-MERCHANT-PAYABLE", null, 12L, 99L, Instant.now()
        );

        listener.onTransactionCompleted(event);

        ArgumentCaptor<PostingRequest> captor = ArgumentCaptor.forClass(PostingRequest.class);
        verify(ledgerPort).post(captor.capture());

        PostingRequest req = captor.getValue();
        assertThat(req.getLines()).hasSize(2);
        assertThat(req.getLines().get(0).getAccountId()).isEqualTo(8L);
        assertThat(req.getLines().get(0).getEntryType()).isEqualTo(EntryType.DEBIT);
        assertThat(req.getLines().get(1).getAccountNo()).isEqualTo("SYS-MERCHANT-PAYABLE");
        assertThat(req.getLines().get(1).getEntryType()).isEqualTo(EntryType.CREDIT);
    }

    @Test
    @DisplayName("Non-financial transactions produce no ledger entries")
    void onTransactionCompleted_balanceInquiry_ignored() {
        TransactionCompletedEvent event = new TransactionCompletedEvent(
                40L, "TXN-BI-1", TransactionType.BALANCE_INQUIRY, "ATM",
                TransactionStatus.APPROVED, BigDecimal.ZERO, "BDT",
                "ACC-101", 1L, null, null, null, null, Instant.now()
        );

        listener.onTransactionCompleted(event);

        verify(ledgerPort, never()).post(any());
    }

    @Test
    @DisplayName("Failed transactions produce no ledger entries")
    void onTransactionCompleted_declined_ignored() {
        TransactionCompletedEvent event = new TransactionCompletedEvent(
                50L, "TXN-FAIL-1", TransactionType.TRANSFER, "WEB",
                TransactionStatus.DECLINED, new BigDecimal("100.00"), "BDT",
                "ACC-101", 1L, "ACC-102", 2L, null, null, Instant.now()
        );

        listener.onTransactionCompleted(event);

        verify(ledgerPort, never()).post(any());
    }
}
