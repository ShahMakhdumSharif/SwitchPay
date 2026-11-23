package com.switchpay.common.port;

import java.math.BigDecimal;

public interface SettlementPort {

    void adjustForRefund(Long merchantId, String txnRef, BigDecimal amount);
}
