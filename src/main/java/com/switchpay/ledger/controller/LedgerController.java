package com.switchpay.ledger.controller;

import com.switchpay.ledger.dto.AccountStatementResponse;
import com.switchpay.ledger.dto.LedgerEntryResponse;
import com.switchpay.ledger.service.LedgerService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ledger")
public class LedgerController {

    private final LedgerService ledgerService;

    public LedgerController(LedgerService ledgerService) {
        this.ledgerService = ledgerService;
    }

    @GetMapping("/entries")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<LedgerEntryResponse>> getEntries(
            @RequestParam("transactionRef") String transactionRef) {
        return ResponseEntity.ok(ledgerService.getEntriesByTransactionRef(transactionRef));
    }

    @GetMapping("/accounts/{no}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AccountStatementResponse> getStatement(
            @PathVariable("no") String accountNo) {
        return ResponseEntity.ok(ledgerService.getAccountStatement(accountNo));
    }
}
