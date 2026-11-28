package com.switchpay.gateway.service;

import com.switchpay.common.exception.NotFoundException;
import com.switchpay.common.exception.UnprocessableException;
import com.switchpay.gateway.dto.CheckoutSessionRequest;
import com.switchpay.gateway.dto.CheckoutSessionResponse;
import com.switchpay.gateway.entity.CheckoutSession;
import com.switchpay.gateway.entity.CheckoutSessionState;
import com.switchpay.gateway.entity.Merchant;
import com.switchpay.gateway.entity.MerchantStatus;
import com.switchpay.gateway.repository.CheckoutSessionRepository;
import com.switchpay.gateway.repository.MerchantRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CheckoutSessionService {

    private static final int DEFAULT_EXPIRY_MINUTES = 30;

    private final CheckoutSessionRepository checkoutSessionRepository;
    private final MerchantRepository merchantRepository;
    private final Clock clock;

    public CheckoutSessionService(CheckoutSessionRepository checkoutSessionRepository,
                                  MerchantRepository merchantRepository,
                                  Clock clock) {
        this.checkoutSessionRepository = checkoutSessionRepository;
        this.merchantRepository = merchantRepository;
        this.clock = clock;
    }

    // a SELLER creates a payment link for their own merchant
    @Transactional
    public CheckoutSessionResponse create(Long sellerUserId, CheckoutSessionRequest request) {
        Merchant merchant = findSellerMerchant(sellerUserId);
        if (merchant.getStatus() != MerchantStatus.ACTIVE) {
            throw new UnprocessableException("Merchant is not active");
        }

        int minutes = request.expiresInMinutes() != null ? request.expiresInMinutes() : DEFAULT_EXPIRY_MINUTES;
        Instant now = Instant.now(clock);

        CheckoutSession session = new CheckoutSession();
        session.setToken(UUID.randomUUID().toString().replace("-", ""));
        session.setMerchantId(merchant.getId());
        session.setAmount(request.amount());
        session.setCurrency("BDT");
        session.setStatus(CheckoutSessionState.CREATED);
        session.setExpiresAt(now.plus(Duration.ofMinutes(minutes)));
        session.setCreatedAt(now);

        return CheckoutSessionResponse.from(checkoutSessionRepository.save(session));
    }

    // the seller's own sessions, newest first
    @Transactional
    public List<CheckoutSessionResponse> listForSeller(Long sellerUserId) {
        Merchant merchant = findSellerMerchant(sellerUserId);
        Instant now = Instant.now(clock);

        List<CheckoutSession> sessions =
                checkoutSessionRepository.findByMerchantIdOrderByCreatedAtDesc(merchant.getId());

        // CREATED sessions past their expiry time become EXPIRED
        for (CheckoutSession session : sessions) {
            if (session.isExpiredAt(now)) {
                session.expire();
            }
        }

        return sessions.stream()
                .map(CheckoutSessionResponse::from)
                .toList();
    }

    private Merchant findSellerMerchant(Long sellerUserId) {
        return merchantRepository.findByUserId(sellerUserId)
                .orElseThrow(() -> new NotFoundException("Register a merchant first"));
    }
}
