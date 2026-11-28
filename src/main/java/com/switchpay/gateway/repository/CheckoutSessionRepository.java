package com.switchpay.gateway.repository;

import com.switchpay.gateway.entity.CheckoutSession;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CheckoutSessionRepository extends JpaRepository<CheckoutSession, Long> {

    Optional<CheckoutSession> findByToken(String token);

    List<CheckoutSession> findByMerchantIdOrderByCreatedAtDesc(Long merchantId);
}
