package com.switchpay.common.stub;

import com.switchpay.common.dto.HandlerResult;
import com.switchpay.common.dto.TransactionRequest;
import com.switchpay.common.enums.TransactionType;
import com.switchpay.common.port.TransactionHandler;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class StubHandler implements TransactionHandler {

    @Override
    public boolean supports(TransactionType type) {
        return type != null;
    }

    @Override
    public HandlerResult handle(TransactionRequest request) {
        return HandlerResult.approved();
    }
}
