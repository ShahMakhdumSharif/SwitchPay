package com.switchpay.gateway.controller;

import com.switchpay.gateway.dto.CheckoutSessionRequest;
import com.switchpay.gateway.dto.CheckoutSessionResponse;
import com.switchpay.gateway.service.CheckoutSessionService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/checkout-sessions")
public class CheckoutSessionController {

    private final CheckoutSessionService checkoutSessionService;

    public CheckoutSessionController(CheckoutSessionService checkoutSessionService) {
        this.checkoutSessionService = checkoutSessionService;
    }

    @PostMapping
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<CheckoutSessionResponse> create(
            @Valid @RequestBody CheckoutSessionRequest request,
            @AuthenticationPrincipal(expression = "userId") Long userId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(checkoutSessionService.create(userId, request));
    }

    @GetMapping
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<List<CheckoutSessionResponse>> listOwn(
            @AuthenticationPrincipal(expression = "userId") Long userId) {
        return ResponseEntity.ok(checkoutSessionService.listForSeller(userId));
    }
}
