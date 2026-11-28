package com.switchpay.banking.controller;

import com.switchpay.banking.dto.AccountResponse;
import com.switchpay.banking.dto.BalanceResponse;
import com.switchpay.banking.dto.CreateAccountRequest;
import com.switchpay.banking.dto.UpdateAccountRequest;
import com.switchpay.banking.service.AccountService;
import com.switchpay.security.service.SwitchPayPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AccountResponse> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(accountService.createAccount(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('BUYER', 'ADMIN')")
    public ResponseEntity<List<AccountResponse>> listAccounts(
            @AuthenticationPrincipal SwitchPayPrincipal principal) {
        Long userId = null;
        boolean isAdmin = principal != null && principal.hasRole("ADMIN");
        if (!isAdmin && principal != null) {
            userId = principal.getUserId();
        }
        return ResponseEntity.ok(accountService.listAccounts(userId));
    }

    @GetMapping("/{no}")
    @PreAuthorize("hasAnyRole('BUYER', 'ADMIN')")
    public ResponseEntity<AccountResponse> getAccount(
            @PathVariable("no") String accountNo,
            @AuthenticationPrincipal SwitchPayPrincipal principal) {
        Long callerId = principal != null ? principal.getUserId() : null;
        boolean isAdmin = principal != null && principal.hasRole("ADMIN");
        return ResponseEntity.ok(accountService.getAccount(accountNo, callerId, isAdmin));
    }

    @PutMapping("/{no}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AccountResponse> updateAccount(
            @PathVariable("no") String accountNo,
            @Valid @RequestBody UpdateAccountRequest request) {
        return ResponseEntity.ok(accountService.updateAccount(accountNo, request));
    }

    @DeleteMapping("/{no}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteAccount(@PathVariable("no") String accountNo) {
        accountService.closeAccount(accountNo);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{no}/balance")
    @PreAuthorize("hasAnyRole('BUYER', 'ADMIN')")
    public ResponseEntity<BalanceResponse> getBalance(
            @PathVariable("no") String accountNo,
            @AuthenticationPrincipal SwitchPayPrincipal principal) {
        Long callerId = principal != null ? principal.getUserId() : null;
        boolean isAdmin = principal != null && principal.hasRole("ADMIN");
        return ResponseEntity.ok(accountService.getBalance(accountNo, callerId, isAdmin));
    }
}
