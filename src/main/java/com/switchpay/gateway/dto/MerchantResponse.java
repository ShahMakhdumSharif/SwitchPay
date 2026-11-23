package com.switchpay.gateway.dto;

import com.switchpay.gateway.entity.Merchant;
import com.switchpay.gateway.entity.MerchantStatus;
import java.time.Instant;

public record MerchantResponse(
        Long id,
        Long userId,
        String businessName,
        String settlementAccountNo,
        MerchantStatus status,
        Instant createdAt
) {

    public static MerchantResponse from(Merchant merchant) {
        return new MerchantResponse(
                merchant.getId(),
                merchant.getUserId(),
                merchant.getBusinessName(),
                merchant.getSettlementAccountNo(),
                merchant.getStatus(),
                merchant.getCreatedAt()
        );
    }
}
