package com.switchpay.gateway.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CheckoutSessionRequest(
        @NotNull
        @DecimalMin(value = "0.01")
        @Digits(integer = 17, fraction = 2)
        BigDecimal amount,

        // optional, default is 30 minutes
        @Min(1)
        @Max(1440)
        Integer expiresInMinutes
) {
}
