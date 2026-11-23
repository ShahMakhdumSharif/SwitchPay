package com.switchpay.security.repository;

import com.switchpay.security.entity.RevokedToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RevokedTokenRepository extends JpaRepository<RevokedToken, String> {

    boolean existsByTokenId(String tokenId);
}
