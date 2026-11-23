package com.switchpay.common.port;

import com.switchpay.common.dto.HandlerResult;
import com.switchpay.common.dto.TransactionRequest;
import com.switchpay.common.enums.TransactionType;

public interface TransactionHandler {

    boolean supports(TransactionType type);

    HandlerResult handle(TransactionRequest request);

    default HandlerResult handle(TransactionRequest request, String transactionRef) {
        return handle(request);
    }
}
