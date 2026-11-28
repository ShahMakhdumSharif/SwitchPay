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
public class MobileChannelAdapter implements ChannelAdapter {

    // what a customer can do in the mobile banking app
    private static final Set<TransactionType> ALLOWED = Set.of(
            TransactionType.TRANSFER,
            TransactionType.BALANCE_INQUIRY,
            TransactionType.ACCOUNT_VALIDATION
    );

    @Override
    public ChannelType channelType() {
        return ChannelType.MOBILE;
    }

    @Override
    public TransactionRequest toCanonical(ChannelTransactionRequest body, Channel channel, Long actorUserId) {
        if (!ALLOWED.contains(body.type())) {
            throw new UnprocessableException("Mobile app does not support " + body.type());
        }

        boolean isTransfer = body.type() == TransactionType.TRANSFER;
        if (isTransfer && (body.destinationAccountNo() == null || body.destinationAccountNo().isBlank())) {
            throw new UnprocessableException("Transfer needs a destination account");
        }

        return TransactionRequest.builder()
                .type(body.type())
                .channelCode(channel.getCode())
                .channelId(channel.getId())
                .sourceAccountNo(body.sourceAccountNo())
                .destinationAccountNo(isTransfer ? body.destinationAccountNo() : null)
                .amount(body.amount() != null ? body.amount() : BigDecimal.ZERO)
                .idempotencyKey(body.idempotencyKey())
                .actorUserId(actorUserId)
                .build();
    }
}
