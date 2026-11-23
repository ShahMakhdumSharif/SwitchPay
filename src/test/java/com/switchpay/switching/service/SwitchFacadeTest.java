package com.switchpay.switching.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.switchpay.common.dto.HandlerResult;
import com.switchpay.common.dto.TransactionRequest;
import com.switchpay.common.dto.TransactionResponse;
import com.switchpay.common.enums.TransactionStatus;
import com.switchpay.common.enums.TransactionType;
import com.switchpay.common.port.AccountPort;
import com.switchpay.common.port.AuditPort;
import com.switchpay.common.port.TransactionHandler;
import com.switchpay.common.stub.StubHandler;
import com.switchpay.switching.auth.AccountActiveRule;
import com.switchpay.switching.auth.AuthorizationRule;
import com.switchpay.switching.auth.SufficientBalanceRule;
import com.switchpay.switching.auth.TransactionLimitRule;
import com.switchpay.switching.entity.Transaction;
import com.switchpay.switching.repository.TransactionRepository;
import com.switchpay.switching.router.TransactionRouter;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class SwitchFacadeTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-11-22T00:00:00Z");

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountPort accountPort;

    @Mock
    private AuditPort auditPort;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private TransactionHandler bankingHandler;

    @Mock
    private TransactionHandler paymentHandler;

    private Clock clock;
    private SwitchFacade switchFacade;
    private TransactionRouter transactionRouter;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
        List<AuthorizationRule> rules = List.of(
                new AccountActiveRule(accountPort),
                new TransactionLimitRule(new BigDecimal("100000.00")),
                new SufficientBalanceRule(accountPort)
        );
        transactionRouter = new TransactionRouter(List.of(bankingHandler, paymentHandler, new StubHandler()));
        switchFacade = new SwitchFacade(
                transactionRepository,
                rules,
                transactionRouter,
                eventPublisher,
                auditPort,
                clock
        );
    }

    @Test
    void authChain_insufficientBalance_declines() {
        TransactionRequest request = TransactionRequest.builder()
                .idempotencyKey("idem-insufficient-1")
                .channelCode("ATM")
                .type(TransactionType.CASH_WITHDRAWAL)
                .sourceAccountNo("ACC-1001")
                .amount(new BigDecimal("5000.00"))
                .actorUserId(10L)
                .build();

        when(transactionRepository.findByIdempotencyKey("idem-insufficient-1")).thenReturn(Optional.empty());
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> {
            Transaction t = inv.getArgument(0);
            if (t.getId() == null) {
                t.setId(1L);
            }
            return t;
        });
        when(accountPort.validate("ACC-1001")).thenReturn(true);
        when(accountPort.balanceOf("ACC-1001")).thenReturn(new BigDecimal("1200.00"));

        TransactionResponse response = switchFacade.submit(request);

        assertThat(response.getStatus()).isEqualTo(TransactionStatus.DECLINED);
        assertThat(response.getDeclineReason()).containsIgnoringCase("Insufficient balance");
    }

    @Test
    void authChain_inactiveAccount_declines() {
        TransactionRequest request = TransactionRequest.builder()
                .idempotencyKey("idem-inactive-1")
                .channelCode("ATM")
                .type(TransactionType.CASH_WITHDRAWAL)
                .sourceAccountNo("ACC-FROZEN")
                .amount(new BigDecimal("1000.00"))
                .actorUserId(10L)
                .build();

        when(transactionRepository.findByIdempotencyKey("idem-inactive-1")).thenReturn(Optional.empty());
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> {
            Transaction t = inv.getArgument(0);
            if (t.getId() == null) {
                t.setId(2L);
            }
            return t;
        });
        when(accountPort.validate("ACC-FROZEN")).thenReturn(false);

        TransactionResponse response = switchFacade.submit(request);

        assertThat(response.getStatus()).isEqualTo(TransactionStatus.DECLINED);
        assertThat(response.getDeclineReason()).containsIgnoringCase("inactive");
    }

    @Test
    void router_withdrawal_selectsBankingHandler() {
        when(bankingHandler.supports(TransactionType.CASH_WITHDRAWAL)).thenReturn(true);
        when(bankingHandler.handle(any(TransactionRequest.class), any())).thenReturn(HandlerResult.approved());

        TransactionHandler resolved = transactionRouter.resolve(TransactionType.CASH_WITHDRAWAL);
        assertThat(resolved).isSameAs(bankingHandler);

        TransactionRequest request = TransactionRequest.builder()
                .idempotencyKey("idem-withdraw-ok")
                .channelCode("ATM")
                .type(TransactionType.CASH_WITHDRAWAL)
                .sourceAccountNo("ACC-1001")
                .amount(new BigDecimal("500.00"))
                .actorUserId(10L)
                .build();

        when(transactionRepository.findByIdempotencyKey("idem-withdraw-ok")).thenReturn(Optional.empty());
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> {
            Transaction t = inv.getArgument(0);
            if (t.getId() == null) {
                t.setId(3L);
            }
            return t;
        });
        when(accountPort.validate("ACC-1001")).thenReturn(true);
        when(accountPort.balanceOf("ACC-1001")).thenReturn(new BigDecimal("2000.00"));

        TransactionResponse response = switchFacade.submit(request);

        assertThat(response.getStatus()).isEqualTo(TransactionStatus.APPROVED);
        verify(bankingHandler).handle(any(TransactionRequest.class), any());
    }
}
