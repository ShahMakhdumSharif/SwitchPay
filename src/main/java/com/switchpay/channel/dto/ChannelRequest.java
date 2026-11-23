package com.switchpay.channel.dto;

import com.switchpay.channel.entity.ChannelStatus;
import com.switchpay.common.enums.ChannelType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ChannelRequest(
        @NotBlank
        @Pattern(regexp = "^[A-Z0-9_-]{2,32}$", message = "must be 2-32 upper case letters, digits, _ or -")
        String code,

        @NotNull
        ChannelType type,

        @NotBlank
        @Size(max = 100)
        String name,

        @Size(max = 255)
        String location,

        // optional, a new channel starts ACTIVE when this is empty
        ChannelStatus status
) {
}
