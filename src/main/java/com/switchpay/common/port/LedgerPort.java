package com.switchpay.common.port;

import com.switchpay.common.dto.PostingRequest;

public interface LedgerPort {

    void post(PostingRequest request);

    void reverse(String txnRef, String adjustmentRef);
}
