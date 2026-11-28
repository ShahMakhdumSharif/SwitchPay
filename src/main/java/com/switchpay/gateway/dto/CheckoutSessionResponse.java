package com.switchpay.gateway.dto;

import com.switchpay.gateway.entity.CheckoutSession;
import com.switchpay.gateway.entity.CheckoutSessionState;
import java.math.BigDecimal;
import java.time.Instant;

public record CheckoutSessionResponse(
        Long id,
        String token,
        Long merchantId,
        BigDecimal amount,
        String currency,
        CheckoutSessionState status,
        Instant expiresAt,
        Instant createdAt
) {

    public static CheckoutSessionResponse from(CheckoutSession session) {
        return new CheckoutSessionResponse(
                session.getId(),
                session.getToken(),
                session.getMerchantId(),
                session.getAmount(),
                session.getCurrency(),
                session.getStatus(),
                session.getExpiresAt(),
                session.getCreatedAt()
        );
    }
}
