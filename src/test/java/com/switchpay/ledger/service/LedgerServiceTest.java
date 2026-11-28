package com.switchpay.ledger.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.switchpay.banking.entity.Account;
import com.switchpay.banking.repository.AccountRepository;
import com.switchpay.common.dto.PostingRequest;
import com.switchpay.common.dto.TransactionResponse;
import com.switchpay.common.enums.EntryType;
import com.switchpay.common.exception.UnbalancedPostingException;
import com.switchpay.common.port.TransactionQueryPort;
import com.switchpay.ledger.dto.AccountStatementResponse;
import com.switchpay.ledger.dto.LedgerEntryResponse;
import com.switchpay.ledger.entity.LedgerEntry;
import com.switchpay.ledger.repository.LedgerEntryRepository;
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
class LedgerServiceTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-11-23T10:00:00Z");

    @Mock
    private LedgerEntryRepository ledgerEntryRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionQueryPort transactionQueryPort;

    private Clock clock;
    private LedgerService ledgerService;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
        ledgerService = new LedgerService(ledgerEntryRepository, accountRepository, transactionQueryPort, clock);
    }

    @Test
    @DisplayName("U5: Balanced posting request saves all entries")
    void ledgerPost_balancedLines_savesEntries() {
        PostingRequest request = PostingRequest.builder()
                .transactionId(100L)
                .line(1L, "ACC-001", EntryType.DEBIT, new BigDecimal("250.00"))
                .line(2L, "ACC-002", EntryType.CREDIT, new BigDecimal("250.00"))
                .build();

        ledgerService.post(request);

        ArgumentCaptor<LedgerEntry> captor = ArgumentCaptor.forClass(LedgerEntry.class);
        verify(ledgerEntryRepository, times(2)).save(captor.capture());

        List<LedgerEntry> saved = captor.getAllValues();
        assertThat(saved).hasSize(2);

        LedgerEntry debitEntry = saved.get(0);
        assertThat(debitEntry.getTransactionId()).isEqualTo(100L);
        assertThat(debitEntry.getAccountId()).isEqualTo(1L);
        assertThat(debitEntry.getEntryType()).isEqualTo(EntryType.DEBIT);
        assertThat(debitEntry.getAmount()).isEqualByComparingTo("250.00");
        assertThat(debitEntry.getCreatedAt()).isEqualTo(FIXED_NOW);

        LedgerEntry creditEntry = saved.get(1);
        assertThat(creditEntry.getTransactionId()).isEqualTo(100L);
        assertThat(creditEntry.getAccountId()).isEqualTo(2L);
        assertThat(creditEntry.getEntryType()).isEqualTo(EntryType.CREDIT);
        assertThat(creditEntry.getAmount()).isEqualByComparingTo("250.00");
        assertThat(creditEntry.getCreatedAt()).isEqualTo(FIXED_NOW);
    }

    @Test
    @DisplayName("U6: Unbalanced posting request throws UnbalancedPostingException")
    void ledgerPost_unbalanced_throwsUnbalancedPosting() {
        PostingRequest request = PostingRequest.builder()
                .transactionId(101L)
                .line(1L, "ACC-001", EntryType.DEBIT, new BigDecimal("300.00"))
                .line(2L, "ACC-002", EntryType.CREDIT, new BigDecimal("150.00"))
                .build();

        assertThatThrownBy(() -> ledgerService.post(request))
                .isInstanceOf(UnbalancedPostingException.class)
                .hasMessageContaining("Unbalanced posting");

        verify(ledgerEntryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Reverse creates opposing entries for an existing transaction")
    void reverse_createsOpposingEntries() {
        String txnRef = "TXN-REV-001";
        TransactionResponse txn = TransactionResponse.builder()
                .transactionId(200L)
                .reference(txnRef)
                .amount(new BigDecimal("100.00"))
                .currency("BDT")
                .sourceAccountNo("ACC-SRC")
                .destinationAccountNo("ACC-DST")
                .createdAt(FIXED_NOW)
                .build();

        when(transactionQueryPort.findByRef(txnRef)).thenReturn(Optional.of(txn));

        LedgerEntry debit = LedgerEntry.builder()
                .id(1L)
                .transactionId(200L)
                .accountId(10L)
                .entryType(EntryType.DEBIT)
                .amount(new BigDecimal("100.00"))
                .createdAt(FIXED_NOW)
                .build();

        LedgerEntry credit = LedgerEntry.builder()
                .id(2L)
                .transactionId(200L)
                .accountId(20L)
                .entryType(EntryType.CREDIT)
                .amount(new BigDecimal("100.00"))
                .createdAt(FIXED_NOW)
                .build();

        when(ledgerEntryRepository.findByTransactionId(200L)).thenReturn(List.of(debit, credit));

        ledgerService.reverse(txnRef, "ADJ-001");

        ArgumentCaptor<LedgerEntry> captor = ArgumentCaptor.forClass(LedgerEntry.class);
        verify(ledgerEntryRepository, times(2)).save(captor.capture());

        List<LedgerEntry> reversed = captor.getAllValues();
        assertThat(reversed.get(0).getEntryType()).isEqualTo(EntryType.CREDIT);
        assertThat(reversed.get(0).getAccountId()).isEqualTo(10L);
        assertThat(reversed.get(1).getEntryType()).isEqualTo(EntryType.DEBIT);
        assertThat(reversed.get(1).getAccountId()).isEqualTo(20L);
    }

    @Test
    @DisplayName("getAccountStatement computes net change correctly")
    void getAccountStatement_computesBalance() {
        Account account = new Account("ACC-123", 5L, null, new BigDecimal("1000.00"), null, "BDT");
        account.setId(50L);

        when(accountRepository.findByAccountNo("ACC-123")).thenReturn(Optional.of(account));

        LedgerEntry debit = LedgerEntry.builder()
                .id(1L)
                .transactionId(10L)
                .accountId(50L)
                .entryType(EntryType.DEBIT)
                .amount(new BigDecimal("200.00"))
                .createdAt(FIXED_NOW)
                .build();

        LedgerEntry credit = LedgerEntry.builder()
                .id(2L)
                .transactionId(11L)
                .accountId(50L)
                .entryType(EntryType.CREDIT)
                .amount(new BigDecimal("500.00"))
                .createdAt(FIXED_NOW)
                .build();

        when(ledgerEntryRepository.findByAccountIdOrderByCreatedAtDesc(50L)).thenReturn(List.of(credit, debit));

        AccountStatementResponse statement = ledgerService.getAccountStatement("ACC-123");

        assertThat(statement.getAccountNo()).isEqualTo("ACC-123");
        assertThat(statement.getTotalDebits()).isEqualByComparingTo("200.00");
        assertThat(statement.getTotalCredits()).isEqualByComparingTo("500.00");
        assertThat(statement.getNetChange()).isEqualByComparingTo("300.00");
        assertThat(statement.getEntries()).hasSize(2);
    }
}
