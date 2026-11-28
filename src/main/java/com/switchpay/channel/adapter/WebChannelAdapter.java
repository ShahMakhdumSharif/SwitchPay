package com.switchpay.channel.adapter;

import com.switchpay.channel.dto.ChannelTransactionRequest;
import com.switchpay.channel.entity.Channel;
import com.switchpay.common.dto.TransactionRequest;
import com.switchpay.common.enums.ChannelType;
import com.switchpay.common.enums.TransactionType;
import com.switchpay.common.exception.UnprocessableException;
import org.springframework.stereotype.Component;

@Component
public class WebChannelAdapter implements ChannelAdapter {

    // money from a merchant checkout goes to the merchant payable system account
    private static final String MERCHANT_PAYABLE_ACCOUNT = "SYS-MERCHANT-PAYABLE";

    @Override
    public ChannelType channelType() {
        return ChannelType.WEB;
    }

    @Override
    public TransactionRequest toCanonical(ChannelTransactionRequest body, Channel channel, Long actorUserId) {
        if (body.type() != TransactionType.MERCHANT_PAYMENT) {
            throw new UnprocessableException("Web checkout only supports MERCHANT_PAYMENT");
        }

        return TransactionRequest.builder()
                .type(TransactionType.MERCHANT_PAYMENT)
                .channelCode(channel.getCode())
                .channelId(channel.getId())
                .sourceAccountNo(body.sourceAccountNo())
                .destinationAccountNo(MERCHANT_PAYABLE_ACCOUNT)
                .amount(body.amount())
                .idempotencyKey(body.idempotencyKey())
                .actorUserId(actorUserId)
                .build();
    }
}
