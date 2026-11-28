package com.switchpay.channel.service;

import com.switchpay.channel.adapter.ChannelAdapter;
import com.switchpay.channel.adapter.ChannelAdapterFactory;
import com.switchpay.channel.dto.ChannelTransactionRequest;
import com.switchpay.channel.entity.Channel;
import com.switchpay.common.dto.TransactionRequest;
import com.switchpay.common.dto.TransactionResponse;
import com.switchpay.common.enums.ChannelType;
import com.switchpay.common.exception.UnprocessableException;
import com.switchpay.common.port.SwitchPort;
import org.springframework.stereotype.Service;

/**
 * Takes a channel payload, finds the right adapter, builds the canonical request
 * and hands it to the switch through SwitchPort (we never call the switch classes directly).
 */
@Service
public class ChannelTransactionService {

    private final ChannelAdapterFactory adapterFactory;
    private final SwitchPort switchPort;

    public ChannelTransactionService(ChannelAdapterFactory adapterFactory, SwitchPort switchPort) {
        this.adapterFactory = adapterFactory;
        this.switchPort = switchPort;
    }

    public TransactionResponse submit(ChannelType expectedType, ChannelTransactionRequest body, Long actorUserId) {
        Channel channel = adapterFactory.getActiveChannel(body.channelCode());

        // e.g. a MOBILE channel code sent to the ATM endpoint
        if (channel.getType() != expectedType) {
            throw new UnprocessableException("Channel " + channel.getCode() + " is not an " + expectedType + " channel");
        }

        ChannelAdapter adapter = adapterFactory.getAdapter(channel);
        TransactionRequest canonical = adapter.toCanonical(body, channel, actorUserId);

        // APPROVED and DECLINED both come back as a normal response
        return switchPort.submit(canonical);
    }
}
