package com.switchpay.channel.controller;

import com.switchpay.channel.dto.ChannelTransactionRequest;
import com.switchpay.channel.service.ChannelTransactionService;
import com.switchpay.common.dto.TransactionResponse;
import com.switchpay.common.enums.ChannelType;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/channels")
public class ChannelTransactionController {

    private final ChannelTransactionService channelTransactionService;

    public ChannelTransactionController(ChannelTransactionService channelTransactionService) {
        this.channelTransactionService = channelTransactionService;
    }

    // withdrawal, balance inquiry, account validation
    @PostMapping("/atm/transactions")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<TransactionResponse> atm(
            @Valid @RequestBody ChannelTransactionRequest body,
            @AuthenticationPrincipal(expression = "userId") Long userId) {
        return ResponseEntity.ok(channelTransactionService.submit(ChannelType.ATM, body, userId));
    }

    // transfer, balance inquiry, account validation
    @PostMapping("/mobile/transactions")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<TransactionResponse> mobile(
            @Valid @RequestBody ChannelTransactionRequest body,
            @AuthenticationPrincipal(expression = "userId") Long userId) {
        return ResponseEntity.ok(channelTransactionService.submit(ChannelType.MOBILE, body, userId));
    }
}
