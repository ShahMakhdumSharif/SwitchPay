package com.switchpay.ledger.service;

import com.switchpay.banking.entity.Account;
import com.switchpay.banking.repository.AccountRepository;
import com.switchpay.common.dto.PostingRequest;
import com.switchpay.common.dto.TransactionResponse;
import com.switchpay.common.enums.EntryType;
import com.switchpay.common.exception.BadRequestException;
import com.switchpay.common.exception.NotFoundException;
import com.switchpay.common.exception.UnbalancedPostingException;
import com.switchpay.common.port.LedgerPort;
import com.switchpay.common.port.TransactionQueryPort;
import com.switchpay.ledger.dto.AccountStatementResponse;
import com.switchpay.ledger.dto.LedgerEntryResponse;
import com.switchpay.ledger.entity.LedgerEntry;
import com.switchpay.ledger.repository.LedgerEntryRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LedgerService implements LedgerPort {

    private final LedgerEntryRepository ledgerEntryRepository;
    private final AccountRepository accountRepository;
    private final TransactionQueryPort transactionQueryPort;
    private final Clock clock;

    public LedgerService(LedgerEntryRepository ledgerEntryRepository,
                         AccountRepository accountRepository,
                         TransactionQueryPort transactionQueryPort,
                         Clock clock) {
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.accountRepository = accountRepository;
        this.transactionQueryPort = transactionQueryPort;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void post(PostingRequest request) {
        if (request == null || request.getLines() == null || request.getLines().isEmpty()) {
            throw new BadRequestException("Posting request must contain at least one line");
        }

        BigDecimal totalDebits = BigDecimal.ZERO;
        BigDecimal totalCredits = BigDecimal.ZERO;

        for (PostingRequest.PostingLine line : request.getLines()) {
            if (line.getAmount() == null || line.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException("Posting line amount must be positive");
            }
            if (line.getEntryType() == EntryType.DEBIT) {
                totalDebits = totalDebits.add(line.getAmount());
            } else if (line.getEntryType() == EntryType.CREDIT) {
                totalCredits = totalCredits.add(line.getAmount());
            }
        }

        if (totalDebits.compareTo(totalCredits) != 0) {
            throw new UnbalancedPostingException(
                    "Unbalanced posting: debits (" + totalDebits + ") != credits (" + totalCredits + ")");
        }

        Long transactionId = request.getTransactionId();
        if (transactionId == null && request.getTransactionRef() != null) {
            TransactionResponse txn = transactionQueryPort.findByRef(request.getTransactionRef())
                    .orElseThrow(() -> new NotFoundException("Transaction not found: " + request.getTransactionRef()));
            transactionId = txn.getTransactionId();
        }

        if (transactionId == null) {
            throw new BadRequestException("Transaction ID or Reference is required for ledger posting");
        }

        Instant now = Instant.now(clock);
        for (PostingRequest.PostingLine line : request.getLines()) {
            Long accountId = line.getAccountId();
            if (accountId == null && line.getAccountNo() != null) {
                Account account = accountRepository.findByAccountNo(line.getAccountNo().trim())
                        .orElseThrow(() -> new NotFoundException("Account not found: " + line.getAccountNo()));
                accountId = account.getId();
            }

            if (accountId == null) {
                throw new BadRequestException("Account ID or Account number is required for posting line");
            }

            LedgerEntry entry = LedgerEntry.builder()
                    .transactionId(transactionId)
                    .accountId(accountId)
                    .entryType(line.getEntryType())
                    .amount(line.getAmount())
                    .adjustmentId(request.getAdjustmentId())
                    .createdAt(now)
                    .build();

            ledgerEntryRepository.save(entry);
        }
    }

    @Override
    @Transactional
    public void reverse(String txnRef, String adjustmentRef) {
        TransactionResponse txn = transactionQueryPort.findByRef(txnRef)
                .orElseThrow(() -> new NotFoundException("Transaction not found: " + txnRef));

        List<LedgerEntry> originalEntries = ledgerEntryRepository.findByTransactionId(txn.getTransactionId());
        if (originalEntries.isEmpty()) {
            throw new NotFoundException("No ledger entries found to reverse for transaction: " + txnRef);
        }

        PostingRequest.Builder reversalBuilder = PostingRequest.builder()
                .transactionId(txn.getTransactionId())
                .transactionRef(txnRef);

        for (LedgerEntry entry : originalEntries) {
            // Flip the entry type: DEBIT -> CREDIT, CREDIT -> DEBIT
            EntryType opposingType = (entry.getEntryType() == EntryType.DEBIT) ? EntryType.CREDIT : EntryType.DEBIT;
            reversalBuilder.line(entry.getAccountId(), null, opposingType, entry.getAmount());
        }

        post(reversalBuilder.build());
    }

    @Transactional(readOnly = true)
    public List<LedgerEntryResponse> getEntriesByTransactionRef(String transactionRef) {
        TransactionResponse txn = transactionQueryPort.findByRef(transactionRef)
                .orElseThrow(() -> new NotFoundException("Transaction not found: " + transactionRef));

        List<LedgerEntry> entries = ledgerEntryRepository.findByTransactionId(txn.getTransactionId());
        List<LedgerEntryResponse> responses = new ArrayList<>();
        for (LedgerEntry entry : entries) {
            String accountNo = accountRepository.findById(entry.getAccountId())
                    .map(Account::getAccountNo)
                    .orElse("UNKNOWN");
            responses.add(LedgerEntryResponse.fromEntity(entry, accountNo));
        }
        return responses;
    }

    @Transactional(readOnly = true)
    public AccountStatementResponse getAccountStatement(String accountNo) {
        Account account = accountRepository.findByAccountNo(accountNo)
                .orElseThrow(() -> new NotFoundException("Account not found: " + accountNo));

        List<LedgerEntry> entries = ledgerEntryRepository.findByAccountIdOrderByCreatedAtDesc(account.getId());
        BigDecimal totalDebits = BigDecimal.ZERO;
        BigDecimal totalCredits = BigDecimal.ZERO;

        List<LedgerEntryResponse> entryResponses = new ArrayList<>();
        for (LedgerEntry entry : entries) {
            if (entry.getEntryType() == EntryType.DEBIT) {
                totalDebits = totalDebits.add(entry.getAmount());
            } else {
                totalCredits = totalCredits.add(entry.getAmount());
            }
            entryResponses.add(LedgerEntryResponse.fromEntity(entry, account.getAccountNo()));
        }

        BigDecimal netChange = totalCredits.subtract(totalDebits);
        return new AccountStatementResponse(account.getAccountNo(), account.getId(), totalDebits, totalCredits, netChange, entryResponses);
    }
}
