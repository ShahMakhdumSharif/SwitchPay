package com.switchpay.channel.adapter;

import com.switchpay.channel.entity.Channel;
import com.switchpay.channel.entity.ChannelStatus;
import com.switchpay.channel.repository.ChannelRepository;
import com.switchpay.common.enums.ChannelType;
import com.switchpay.common.exception.UnprocessableException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Factory pattern: give it a channel code ("ATM", "MOBILE", "WEB" ...)
 * and it gives back the active channel and the adapter that knows its payload.
 * Unknown or inactive channel = 422.
 */
@Component
public class ChannelAdapterFactory {

    private final ChannelRepository channelRepository;
    private final Map<ChannelType, ChannelAdapter> adapters = new EnumMap<>(ChannelType.class);

    // Spring passes every ChannelAdapter bean (ATM, mobile, web)
    public ChannelAdapterFactory(ChannelRepository channelRepository, List<ChannelAdapter> adapterList) {
        this.channelRepository = channelRepository;
        for (ChannelAdapter adapter : adapterList) {
            adapters.put(adapter.channelType(), adapter);
        }
    }

    public Channel getActiveChannel(String channelCode) {
        Channel channel = channelRepository.findByCode(channelCode)
                .orElseThrow(() -> new UnprocessableException("Unknown channel: " + channelCode));

        if (channel.getStatus() != ChannelStatus.ACTIVE) {
            throw new UnprocessableException("Channel is not active: " + channelCode);
        }
        return channel;
    }

    public ChannelAdapter getAdapter(String channelCode) {
        return getAdapter(getActiveChannel(channelCode));
    }

    public ChannelAdapter getAdapter(Channel channel) {
        return adapters.get(channel.getType());
    }
}
