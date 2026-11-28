package com.switchpay.adjustment.repository;

import com.switchpay.adjustment.entity.TransactionAdjustment;
import com.switchpay.common.enums.AdjustmentType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionAdjustmentRepository extends JpaRepository<TransactionAdjustment, Long> {

    List<TransactionAdjustment> findByOriginalTransactionId(Long originalTransactionId);

    List<TransactionAdjustment> findByOriginalTransactionIdAndType(Long originalTransactionId, AdjustmentType type);

    boolean existsByOriginalTransactionIdAndTypeAndStatus(Long originalTransactionId, AdjustmentType type, String status);
}
