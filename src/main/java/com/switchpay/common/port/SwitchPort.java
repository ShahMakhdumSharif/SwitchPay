package com.switchpay.common.port;

import com.switchpay.common.dto.TransactionRequest;
import com.switchpay.common.dto.TransactionResponse;

public interface SwitchPort {

    TransactionResponse submit(TransactionRequest request);
}
