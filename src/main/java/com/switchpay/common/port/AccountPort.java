package com.switchpay.common.port;

import java.math.BigDecimal;

public interface AccountPort {

    boolean validate(String accountNo);

    BigDecimal balanceOf(String accountNo);

    void debit(String accountNo, BigDecimal amount, String txnRef);

    void credit(String accountNo, BigDecimal amount, String txnRef);
}
