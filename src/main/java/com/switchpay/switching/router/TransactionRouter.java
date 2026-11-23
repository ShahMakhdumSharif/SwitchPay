package com.switchpay.switching.router;

import com.switchpay.common.enums.TransactionType;
import com.switchpay.common.exception.UnprocessableException;
import com.switchpay.common.port.TransactionHandler;
import com.switchpay.common.stub.StubHandler;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class TransactionRouter {

    private final List<TransactionHandler> handlers;

    public TransactionRouter(List<TransactionHandler> handlers) {
        this.handlers = handlers;
    }

    public TransactionHandler resolve(TransactionType type) {
        if (type == null) {
            throw new UnprocessableException("Transaction type is required");
        }
        TransactionHandler fallbackStub = null;
        for (TransactionHandler handler : handlers) {
            if (handler.supports(type)) {
                if (handler instanceof StubHandler) {
                    fallbackStub = handler;
                } else {
                    return handler;
                }
            }
        }
        if (fallbackStub != null) {
            return fallbackStub;
        }
        throw new UnprocessableException("No transaction handler registered for type: " + type);
    }
}
