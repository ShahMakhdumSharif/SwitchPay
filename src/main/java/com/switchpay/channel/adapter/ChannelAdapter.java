package com.switchpay.channel.adapter;

import com.switchpay.channel.dto.ChannelTransactionRequest;
import com.switchpay.channel.entity.Channel;
import com.switchpay.common.dto.TransactionRequest;
import com.switchpay.common.enums.ChannelType;

/**
 * Adapter pattern: every channel (ATM, mobile, web) sends its own payload,
 * the adapter turns it into the one canonical TransactionRequest the switch understands.
 */
public interface ChannelAdapter {

    ChannelType channelType();

    TransactionRequest toCanonical(ChannelTransactionRequest body, Channel channel, Long actorUserId);
}
