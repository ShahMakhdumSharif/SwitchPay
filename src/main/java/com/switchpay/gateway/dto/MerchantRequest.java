package com.switchpay.gateway.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MerchantRequest(
        @NotBlank
        @Size(max = 150)
        String businessName,

        @NotBlank
        @Size(max = 34)
        String settlementAccountNo
) {
}
