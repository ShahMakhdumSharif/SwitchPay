package com.switchpay.adjustment.controller;

import com.switchpay.adjustment.dto.AdjustmentResponse;
import com.switchpay.adjustment.dto.RefundRequest;
import com.switchpay.adjustment.dto.ReversalRequest;
import com.switchpay.adjustment.service.AdjustmentService;
import com.switchpay.security.service.SwitchPayPrincipal;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/adjustments")
public class AdjustmentController {

    private final AdjustmentService adjustmentService;

    public AdjustmentController(AdjustmentService adjustmentService) {
        this.adjustmentService = adjustmentService;
    }

    @PostMapping("/refunds")
    @PreAuthorize("hasAnyRole('ADMIN', 'SELLER')")
    public ResponseEntity<AdjustmentResponse> refund(
            @Valid @RequestBody RefundRequest request,
            @AuthenticationPrincipal SwitchPayPrincipal principal) {
        Long actorUserId = principal != null ? principal.getUserId() : null;
        AdjustmentResponse response = adjustmentService.processRefund(request, actorUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/reversals")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdjustmentResponse> reversal(
            @Valid @RequestBody ReversalRequest request,
            @AuthenticationPrincipal SwitchPayPrincipal principal) {
        Long actorUserId = principal != null ? principal.getUserId() : null;
        AdjustmentResponse response = adjustmentService.processReversal(request, actorUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AdjustmentResponse>> listAdjustments(
            @RequestParam(value = "transactionRef", required = false) String transactionRef) {
        return ResponseEntity.ok(adjustmentService.getAdjustments(transactionRef));
    }
}
