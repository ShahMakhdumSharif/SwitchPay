package com.switchpay.channel.dto;

import com.switchpay.common.enums.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

// C4 body sent by the ATM screen and the mobile app screen
public record ChannelTransactionRequest(
        @NotBlank
        String channelCode,

        @NotNull
        TransactionType type,

        @NotBlank
        String sourceAccountNo,

        // only needed for TRANSFER
        String destinationAccountNo,

        // can be empty for BALANCE_INQUIRY and ACCOUNT_VALIDATION
        @DecimalMin(value = "0.00")
        BigDecimal amount,

        // the browser makes it with crypto.randomUUID() and reuses it on retry
        @NotBlank
        String idempotencyKey
) {
}
