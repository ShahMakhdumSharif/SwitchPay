package com.switchpay.gateway.repository;

import com.switchpay.gateway.entity.Merchant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MerchantRepository extends JpaRepository<Merchant, Long> {

    boolean existsByUserId(Long userId);

    Optional<Merchant> findByUserId(Long userId);
}
