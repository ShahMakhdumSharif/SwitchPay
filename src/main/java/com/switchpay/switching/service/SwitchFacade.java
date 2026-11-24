package com.switchpay.switching.service;

import com.switchpay.common.dto.HandlerResult;
import com.switchpay.common.dto.TransactionFilter;
import com.switchpay.common.dto.TransactionRequest;
import com.switchpay.common.dto.TransactionResponse;
import com.switchpay.common.enums.TransactionStatus;
import com.switchpay.common.event.TransactionCompletedEvent;
import com.switchpay.common.exception.BadRequestException;
import com.switchpay.common.exception.ForbiddenException;
import com.switchpay.common.exception.NotFoundException;
import com.switchpay.common.port.AuditPort;
import com.switchpay.common.port.SwitchPort;
import com.switchpay.common.port.TransactionHandler;
import com.switchpay.common.port.TransactionQueryPort;
import com.switchpay.switching.auth.AuthorizationDecision;
import com.switchpay.switching.auth.AuthorizationRule;
import com.switchpay.switching.entity.Transaction;
import com.switchpay.switching.repository.TransactionRepository;
import com.switchpay.switching.router.TransactionRouter;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SwitchFacade implements SwitchPort, TransactionQueryPort {

    private final TransactionRepository transactionRepository;
    private final List<AuthorizationRule> authorizationRules;
    private final TransactionRouter transactionRouter;
    private final ApplicationEventPublisher eventPublisher;
    private final AuditPort auditPort;
    private final Clock clock;

    public SwitchFacade(
            TransactionRepository transactionRepository,
            List<AuthorizationRule> authorizationRules,
            TransactionRouter transactionRouter,
            ApplicationEventPublisher eventPublisher,
            AuditPort auditPort,
            Clock clock) {
        this.transactionRepository = transactionRepository;
        this.authorizationRules = authorizationRules;
        this.transactionRouter = transactionRouter;
        this.eventPublisher = eventPublisher;
        this.auditPort = auditPort;
        this.clock = clock;
    }

    @Override
    @Transactional
    public TransactionResponse submit(TransactionRequest request) {
        validateRequest(request);

        Optional<Transaction> existing = transactionRepository.findByIdempotencyKey(request.getIdempotencyKey().trim());
        if (existing.isPresent()) {
            return toResponse(existing.get(), null);
        }

        Instant now = Instant.now(clock);
        Transaction txn = new Transaction();
        txn.setReference(UUID.randomUUID().toString());
        txn.setIdempotencyKey(request.getIdempotencyKey().trim());
        txn.setType(request.getType());
        txn.setChannelId(request.getChannelId());
        txn.setChannelCode(request.getChannelCode() != null ? request.getChannelCode().trim().toUpperCase() : "WEB");
        txn.setStatus(TransactionStatus.PENDING);
        txn.setAmount(request.getAmount() != null ? request.getAmount() : BigDecimal.ZERO);
        txn.setCurrency(request.getCurrency() != null && !request.getCurrency().isBlank() ? request.getCurrency() : "BDT");
        txn.setSourceAccountId(request.getSourceAccountId());
        txn.setSourceAccountNo(request.getSourceAccountNo());
        txn.setDestinationAccountId(request.getDestinationAccountId());
        txn.setDestinationAccountNo(request.getDestinationAccountNo());
        txn.setMerchantId(request.getMerchantId());
        txn.setCheckoutSessionId(request.getCheckoutSessionId());
        txn.setActorUserId(request.getActorUserId());
        txn.setCreatedAt(now);
        txn.setUpdatedAt(now);

        txn = transactionRepository.save(txn);

        for (AuthorizationRule rule : authorizationRules) {
            AuthorizationDecision decision = rule.evaluate(request);
            if (!decision.isApproved()) {
                txn.setStatus(TransactionStatus.DECLINED);
                txn.setDeclineReason(decision.getDeclineReason());
                txn.setUpdatedAt(Instant.now(clock));
                txn = transactionRepository.save(txn);

                auditPort.record(
                        request.getActorUserId(),
                        "TRANSACTION_DECLINED",
                        txn.getReference(),
                        "Reason=" + decision.getDeclineReason()
                );
                publishCompletedEvent(txn);
                return toResponse(txn, null);
            }
        }

        BigDecimal balanceResult = null;
        try {
            TransactionHandler handler = transactionRouter.resolve(request.getType());
            HandlerResult result = handler.handle(request, txn.getReference());
            txn.setStatus(result.getStatus());
            txn.setDeclineReason(result.getDeclineReason());
            balanceResult = result.getBalance();
        } catch (Exception ex) {
            txn.setStatus(TransactionStatus.FAILED);
            txn.setDeclineReason(ex.getMessage() != null ? ex.getMessage() : "Handler execution failed");
        }

        txn.setUpdatedAt(Instant.now(clock));
        txn = transactionRepository.save(txn);

        auditPort.record(
                request.getActorUserId(),
                "TRANSACTION_" + txn.getStatus().name(),
                txn.getReference(),
                "Type=" + txn.getType() + ",Channel=" + txn.getChannelCode() + ",Amount=" + txn.getAmount()
        );
        publishCompletedEvent(txn);
        return toResponse(txn, balanceResult);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TransactionResponse> findByRef(String ref) {
        return transactionRepository.findByReference(ref).map(t -> toResponse(t, null));
    }

    @Transactional(readOnly = true)
    public TransactionResponse getByRefForActor(String ref, Long callerUserId, boolean isAdmin) {
        Transaction txn = transactionRepository.findByReference(ref)
                .orElseThrow(() -> new NotFoundException("Transaction not found: " + ref));
        if (!isAdmin && (txn.getActorUserId() == null || !txn.getActorUserId().equals(callerUserId))) {
            throw new ForbiddenException("Not authorized to view this transaction");
        }
        return toResponse(txn, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionResponse> search(TransactionFilter filter) {
        return searchPage(filter).getContent();
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> searchPage(TransactionFilter filter) {
        int page = Math.max(0, filter.getPage());
        int size = filter.getSize() > 0 ? filter.getSize() : 20;
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<Transaction> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (filter.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), filter.getStatus()));
            }
            if (filter.getType() != null) {
                predicates.add(cb.equal(root.get("type"), filter.getType()));
            }
            if (filter.getChannelCode() != null && !filter.getChannelCode().isBlank()) {
                predicates.add(cb.equal(
                        cb.upper(root.get("channelCode")),
                        filter.getChannelCode().trim().toUpperCase()
                ));
            }
            if (filter.getChannelId() != null) {
                predicates.add(cb.equal(root.get("channelId"), filter.getChannelId()));
            }
            if (filter.getUserId() != null) {
                predicates.add(cb.equal(root.get("actorUserId"), filter.getUserId()));
            }
            if (filter.getMerchantId() != null) {
                predicates.add(cb.equal(root.get("merchantId"), filter.getMerchantId()));
            }
            if (filter.getFromDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), filter.getFromDate()));
            }
            if (filter.getToDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), filter.getToDate()));
            }
            if (filter.getStuckOlderThanMinutes() != null && filter.getStuckOlderThanMinutes() > 0) {
                Instant cutoff = Instant.now(clock).minus(filter.getStuckOlderThanMinutes(), ChronoUnit.MINUTES);
                predicates.add(cb.equal(root.get("status"), TransactionStatus.PENDING));
                predicates.add(cb.lessThan(root.get("createdAt"), cutoff));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return transactionRepository.findAll(spec, pageable).map(t -> toResponse(t, null));
    }

    @Override
    @Transactional
    public void markStatus(String ref, TransactionStatus status) {
        Transaction txn = transactionRepository.findByReference(ref)
                .orElseThrow(() -> new NotFoundException("Transaction not found: " + ref));
        txn.setStatus(status);
        txn.setUpdatedAt(Instant.now(clock));
        transactionRepository.save(txn);
    }

    private void validateRequest(TransactionRequest request) {
        if (request == null) {
            throw new BadRequestException("Transaction request is required");
        }
        if (request.getIdempotencyKey() == null || request.getIdempotencyKey().isBlank()) {
            throw new BadRequestException("idempotencyKey is required");
        }
        if (request.getType() == null) {
            throw new BadRequestException("Transaction type is required");
        }
    }

    private void publishCompletedEvent(Transaction txn) {
        TransactionCompletedEvent event = new TransactionCompletedEvent(
                txn.getId(),
                txn.getReference(),
                txn.getType(),
                txn.getChannelCode(),
                txn.getStatus(),
                txn.getAmount(),
                txn.getCurrency(),
                txn.getSourceAccountNo(),
                txn.getSourceAccountId(),
                txn.getDestinationAccountNo(),
                txn.getDestinationAccountId(),
                txn.getMerchantId(),
                txn.getCheckoutSessionId(),
                txn.getUpdatedAt()
        );
        eventPublisher.publishEvent(event);
    }

    private TransactionResponse toResponse(Transaction txn, BigDecimal balance) {
        return TransactionResponse.builder()
                .transactionId(txn.getId())
                .reference(txn.getReference())
                .idempotencyKey(txn.getIdempotencyKey())
                .type(txn.getType())
                .channelCode(txn.getChannelCode())
                .channelId(txn.getChannelId())
                .status(txn.getStatus())
                .amount(txn.getAmount())
                .currency(txn.getCurrency())
                .sourceAccountNo(txn.getSourceAccountNo())
                .sourceAccountId(txn.getSourceAccountId())
                .destinationAccountNo(txn.getDestinationAccountNo())
                .destinationAccountId(txn.getDestinationAccountId())
                .merchantId(txn.getMerchantId())
                .checkoutSessionId(txn.getCheckoutSessionId())
                .declineReason(txn.getDeclineReason())
                .balance(balance)
                .createdAt(txn.getCreatedAt())
                .updatedAt(txn.getUpdatedAt())
                .build();
    }
}
