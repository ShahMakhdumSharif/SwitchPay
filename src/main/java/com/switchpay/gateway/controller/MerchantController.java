package com.switchpay.gateway.controller;

import com.switchpay.gateway.dto.MerchantRequest;
import com.switchpay.gateway.dto.MerchantResponse;
import com.switchpay.gateway.service.MerchantService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Merchant CRUD. SELLER works on their own merchant, ADMIN can see and change all.
 * userId comes from the logged-in JWT principal (expression = "userId").
 */
@RestController
@RequestMapping("/api/merchants")
public class MerchantController {

    private final MerchantService merchantService;

    public MerchantController(MerchantService merchantService) {
        this.merchantService = merchantService;
    }

    @PostMapping
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<MerchantResponse> register(
            @Valid @RequestBody MerchantRequest request,
            @AuthenticationPrincipal(expression = "userId") Long userId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(merchantService.register(userId, request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    public ResponseEntity<List<MerchantResponse>> list(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            Authentication authentication) {
        return ResponseEntity.ok(merchantService.list(userId, isAdmin(authentication)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    public ResponseEntity<MerchantResponse> get(
            @PathVariable Long id,
            @AuthenticationPrincipal(expression = "userId") Long userId,
            Authentication authentication) {
        return ResponseEntity.ok(merchantService.getById(id, userId, isAdmin(authentication)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    public ResponseEntity<MerchantResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody MerchantRequest request,
            @AuthenticationPrincipal(expression = "userId") Long userId,
            Authentication authentication) {
        return ResponseEntity.ok(merchantService.update(id, request, userId, isAdmin(authentication)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    public ResponseEntity<Void> close(
            @PathVariable Long id,
            @AuthenticationPrincipal(expression = "userId") Long userId,
            Authentication authentication) {
        merchantService.close(id, userId, isAdmin(authentication));
        return ResponseEntity.noContent().build();
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }
}
