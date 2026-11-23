package com.switchpay.switching.auth;

import com.switchpay.common.dto.TransactionRequest;
import com.switchpay.common.port.AccountPort;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class AccountActiveRule implements AuthorizationRule {

    private final AccountPort accountPort;

    public AccountActiveRule(AccountPort accountPort) {
        this.accountPort = accountPort;
    }

    @Autowired
    public AccountActiveRule(Optional<AccountPort> accountPort) {
        this.accountPort = accountPort.orElse(null);
    }

    @Override
    public AuthorizationDecision evaluate(TransactionRequest request) {
        String sourceAccountNo = request.getSourceAccountNo();
        if (accountPort != null && sourceAccountNo != null && !sourceAccountNo.isBlank()) {
            boolean active = accountPort.validate(sourceAccountNo);
            if (!active) {
                return AuthorizationDecision.decline("Source account is inactive or invalid: " + sourceAccountNo);
            }
        }
        return AuthorizationDecision.approve();
    }
}
