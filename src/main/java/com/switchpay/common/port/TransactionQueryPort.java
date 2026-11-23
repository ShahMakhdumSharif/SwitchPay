package com.switchpay.common.port;

import com.switchpay.common.dto.TransactionFilter;
import com.switchpay.common.dto.TransactionResponse;
import com.switchpay.common.enums.TransactionStatus;
import java.util.List;
import java.util.Optional;

public interface TransactionQueryPort {

    Optional<TransactionResponse> findByRef(String ref);

    List<TransactionResponse> search(TransactionFilter filter);

    void markStatus(String ref, TransactionStatus status);
}
