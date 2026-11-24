package com.switchpay.switching.controller;

import com.switchpay.common.dto.TransactionFilter;
import com.switchpay.common.dto.TransactionRequest;
import com.switchpay.common.dto.TransactionResponse;
import com.switchpay.common.enums.TransactionStatus;
import com.switchpay.security.service.SwitchPayPrincipal;
import com.switchpay.switching.dto.SubmitTransactionRequest;
import com.switchpay.switching.service.SwitchFacade;
import jakarta.validation.Valid;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final SwitchFacade switchFacade;

    public TransactionController(SwitchFacade switchFacade) {
        this.switchFacade = switchFacade;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('BUYER', 'SELLER', 'ADMIN')")
    public ResponseEntity<TransactionResponse> submitTransaction(
            @Valid @RequestBody SubmitTransactionRequest body,
            @AuthenticationPrincipal SwitchPayPrincipal principal) {
        TransactionRequest request = TransactionRequest.builder()
                .channelCode(body.getChannelCode())
                .type(body.getType())
                .sourceAccountNo(body.getSourceAccountNo())
                .destinationAccountNo(body.getDestinationAccountNo())
                .amount(body.getAmount())
                .idempotencyKey(body.getIdempotencyKey())
                .merchantId(body.getMerchantId())
                .checkoutSessionId(body.getCheckoutSessionId())
                .actorUserId(principal != null ? principal.getUserId() : null)
                .build();
        return ResponseEntity.ok(switchFacade.submit(request));
    }

    @GetMapping("/{ref}")
    @PreAuthorize("hasAnyRole('BUYER', 'SELLER', 'ADMIN')")
    public ResponseEntity<TransactionResponse> getTransactionByRef(
            @PathVariable String ref,
            @AuthenticationPrincipal SwitchPayPrincipal principal) {
        Long callerId = principal != null ? principal.getUserId() : null;
        boolean isAdmin = principal != null && principal.hasRole("ADMIN");
        return ResponseEntity.ok(switchFacade.getByRefForActor(ref, callerId, isAdmin));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('BUYER', 'ADMIN')")
    public ResponseEntity<Page<TransactionResponse>> listTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @AuthenticationPrincipal SwitchPayPrincipal principal) {
        TransactionFilter filter = new TransactionFilter();
        filter.setPage(page);
        filter.setSize(size);
        filter.setStatus(status);
        filter.setChannelCode(channel);
        filter.setFromDate(from);
        filter.setToDate(to);

        boolean isAdmin = principal != null && principal.hasRole("ADMIN");
        if (!isAdmin && principal != null) {
            filter.setUserId(principal.getUserId());
        }
        return ResponseEntity.ok(switchFacade.searchPage(filter));
    }
}
