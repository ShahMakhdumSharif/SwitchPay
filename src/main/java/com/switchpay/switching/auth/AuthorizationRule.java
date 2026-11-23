package com.switchpay.switching.auth;

import com.switchpay.common.dto.TransactionRequest;

public interface AuthorizationRule {

    AuthorizationDecision evaluate(TransactionRequest request);
}
