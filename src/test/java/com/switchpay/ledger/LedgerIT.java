package com.switchpay.ledger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.switchpay.banking.entity.Account;
import com.switchpay.banking.enums.AccountStatus;
import com.switchpay.banking.enums.AccountType;
import com.switchpay.banking.repository.AccountRepository;
import com.switchpay.common.dto.PostingRequest;
import com.switchpay.common.enums.EntryType;
import com.switchpay.common.enums.TransactionStatus;
import com.switchpay.common.enums.TransactionType;
import com.switchpay.ledger.dto.LedgerEntryResponse;
import com.switchpay.ledger.service.LedgerService;
import com.switchpay.security.TestAuth;
import com.switchpay.switching.entity.Transaction;
import com.switchpay.switching.repository.TransactionRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class LedgerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestAuth testAuth;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private LedgerService ledgerService;

    @Test
    @DisplayName("IT3: readsLedgerEntries_transactionRef_debitsEqualCredits")
    void readsLedgerEntries_transactionRef_debitsEqualCredits() throws Exception {
        // 1. Create source and destination accounts
        String srcAccNo = "ACC-SRC-" + UUID.randomUUID().toString().substring(0, 8);
        String dstAccNo = "ACC-DST-" + UUID.randomUUID().toString().substring(0, 8);
        Instant now = Instant.now();

        Account srcAccount = accountRepository.save(new Account(
                srcAccNo, 1001L, AccountType.SAVINGS, new BigDecimal("10000.00"), AccountStatus.ACTIVE, "BDT"
        ));
        Account dstAccount = accountRepository.save(new Account(
                dstAccNo, 1002L, AccountType.SAVINGS, new BigDecimal("5000.00"), AccountStatus.ACTIVE, "BDT"
        ));

        // 2. Create transaction
        String txnRef = "TXN-IT3-" + UUID.randomUUID().toString().substring(0, 8);
        Transaction txn = new Transaction();
        txn.setReference(txnRef);
        txn.setIdempotencyKey("IDEMP-" + txnRef);
        txn.setType(TransactionType.TRANSFER);
        txn.setStatus(TransactionStatus.APPROVED);
        txn.setAmount(new BigDecimal("1500.00"));
        txn.setCurrency("BDT");
        txn.setSourceAccountId(srcAccount.getId());
        txn.setSourceAccountNo(srcAccount.getAccountNo());
        txn.setDestinationAccountId(dstAccount.getId());
        txn.setDestinationAccountNo(dstAccount.getAccountNo());
        txn.setCreatedAt(now);
        txn.setUpdatedAt(now);
        txn = transactionRepository.save(txn);

        // 3. Post balanced ledger entries
        PostingRequest postingRequest = PostingRequest.builder()
                .transactionId(txn.getId())
                .transactionRef(txnRef)
                .line(srcAccount.getId(), srcAccount.getAccountNo(), EntryType.DEBIT, new BigDecimal("1500.00"))
                .line(dstAccount.getId(), dstAccount.getAccountNo(), EntryType.CREDIT, new BigDecimal("1500.00"))
                .build();
        ledgerService.post(postingRequest);

        // 4. Admin queries GET /api/ledger/entries?transactionRef=...
        MvcResult result = mockMvc.perform(get("/api/ledger/entries")
                        .param("transactionRef", txnRef)
                        .with(testAuth.asAdmin()))
                .andExpect(status().isOk())
                .andReturn();

        List<LedgerEntryResponse> entries = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<List<LedgerEntryResponse>>() {}
        );

        assertThat(entries).hasSize(2);

        BigDecimal debits = entries.stream()
                .filter(e -> e.getEntryType() == EntryType.DEBIT)
                .map(LedgerEntryResponse::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal credits = entries.stream()
                .filter(e -> e.getEntryType() == EntryType.CREDIT)
                .map(LedgerEntryResponse::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertThat(debits).isEqualByComparingTo("1500.00");
        assertThat(credits).isEqualByComparingTo("1500.00");
        assertThat(debits).isEqualByComparingTo(credits);

        // 5. Verify non-admin (buyer) gets 403 Forbidden
        mockMvc.perform(get("/api/ledger/entries")
                        .param("transactionRef", txnRef)
                        .with(testAuth.asBuyer(1001L, "buyer@test.local")))
                .andExpect(status().isForbidden());
    }
}
