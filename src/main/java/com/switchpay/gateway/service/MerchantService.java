package com.switchpay.gateway.service;

import com.switchpay.common.exception.ConflictException;
import com.switchpay.common.exception.ForbiddenException;
import com.switchpay.common.exception.NotFoundException;
import com.switchpay.gateway.dto.MerchantRequest;
import com.switchpay.gateway.dto.MerchantResponse;
import com.switchpay.gateway.entity.Merchant;
import com.switchpay.gateway.entity.MerchantStatus;
import com.switchpay.gateway.repository.MerchantRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MerchantService {

    private final MerchantRepository merchantRepository;
    private final Clock clock;

    public MerchantService(MerchantRepository merchantRepository, Clock clock) {
        this.merchantRepository = merchantRepository;
        this.clock = clock;
    }

    // a SELLER can own only one merchant (1:1), a second try is 409
    @Transactional
    public MerchantResponse register(Long sellerUserId, MerchantRequest request) {
        if (merchantRepository.existsByUserId(sellerUserId)) {
            throw new ConflictException("Merchant already registered for this seller");
        }

        Merchant merchant = new Merchant();
        merchant.setUserId(sellerUserId);
        merchant.setBusinessName(request.businessName());
        merchant.setSettlementAccountNo(request.settlementAccountNo());
        merchant.setStatus(MerchantStatus.ACTIVE);
        merchant.setCreatedAt(Instant.now(clock));

        return MerchantResponse.from(merchantRepository.save(merchant));
    }

    // ADMIN sees every merchant, a SELLER sees only their own
    @Transactional(readOnly = true)
    public List<MerchantResponse> list(Long currentUserId, boolean isAdmin) {
        if (isAdmin) {
            return merchantRepository.findAll().stream()
                    .map(MerchantResponse::from)
                    .toList();
        }
        return merchantRepository.findByUserId(currentUserId).stream()
                .map(MerchantResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public MerchantResponse getById(Long id, Long currentUserId, boolean isAdmin) {
        return MerchantResponse.from(findOwnedMerchant(id, currentUserId, isAdmin));
    }

    @Transactional
    public MerchantResponse update(Long id, MerchantRequest request, Long currentUserId, boolean isAdmin) {
        Merchant merchant = findOwnedMerchant(id, currentUserId, isAdmin);
        merchant.setBusinessName(request.businessName());
        merchant.setSettlementAccountNo(request.settlementAccountNo());
        return MerchantResponse.from(merchant);
    }

    // soft delete: checkout sessions and settlements keep their merchant
    @Transactional
    public void close(Long id, Long currentUserId, boolean isAdmin) {
        Merchant merchant = findOwnedMerchant(id, currentUserId, isAdmin);
        merchant.setStatus(MerchantStatus.CLOSED);
    }

    private Merchant findOwnedMerchant(Long id, Long currentUserId, boolean isAdmin) {
        Merchant merchant = merchantRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Merchant not found: " + id));

        if (!isAdmin && !merchant.getUserId().equals(currentUserId)) {
            throw new ForbiddenException("You can only access your own merchant");
        }
        return merchant;
    }
}
