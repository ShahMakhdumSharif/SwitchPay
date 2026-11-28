package com.switchpay.channel.adapter;

import com.switchpay.channel.dto.ChannelTransactionRequest;
import com.switchpay.channel.entity.Channel;
import com.switchpay.common.dto.TransactionRequest;
import com.switchpay.common.enums.ChannelType;
import com.switchpay.common.enums.TransactionType;
import com.switchpay.common.exception.UnprocessableException;
import java.math.BigDecimal;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class AtmChannelAdapter implements ChannelAdapter {

    // what a customer can do at an ATM
    private static final Set<TransactionType> ALLOWED = Set.of(
            TransactionType.CASH_WITHDRAWAL,
            TransactionType.BALANCE_INQUIRY,
            TransactionType.ACCOUNT_VALIDATION
    );

    @Override
    public ChannelType channelType() {
        return ChannelType.ATM;
    }

    @Override
    public TransactionRequest toCanonical(ChannelTransactionRequest body, Channel channel, Long actorUserId) {
        if (!ALLOWED.contains(body.type())) {
            throw new UnprocessableException("ATM does not support " + body.type());
        }

        // an ATM has no destination account, the cash leaves the machine
        return TransactionRequest.builder()
                .type(body.type())
                .channelCode(channel.getCode())
                .channelId(channel.getId())
                .sourceAccountNo(body.sourceAccountNo())
                .amount(body.amount() != null ? body.amount() : BigDecimal.ZERO)
                .idempotencyKey(body.idempotencyKey())
                .actorUserId(actorUserId)
                .build();
    }
}
