package com.switchpay.channel.dto;

import com.switchpay.channel.entity.Channel;
import com.switchpay.channel.entity.ChannelStatus;
import com.switchpay.common.enums.ChannelType;
import java.time.Instant;

public record ChannelResponse(
        Long id,
        String code,
        ChannelType type,
        String name,
        String location,
        ChannelStatus status,
        Instant createdAt
) {

    public static ChannelResponse from(Channel channel) {
        return new ChannelResponse(
                channel.getId(),
                channel.getCode(),
                channel.getType(),
                channel.getName(),
                channel.getLocation(),
                channel.getStatus(),
                channel.getCreatedAt()
        );
    }
}
