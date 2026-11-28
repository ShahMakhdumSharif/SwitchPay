package com.switchpay.banking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.switchpay.banking.dto.AccountResponse;
import com.switchpay.banking.dto.CreateAccountRequest;
import com.switchpay.banking.entity.Account;
import com.switchpay.banking.enums.AccountStatus;
import com.switchpay.banking.enums.AccountType;
import com.switchpay.banking.repository.AccountRepository;
import com.switchpay.common.exception.ConflictException;
import com.switchpay.common.exception.UnprocessableException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-11-22T00:00:00Z");

    @Mock
    private AccountRepository accountRepository;

    private Clock clock;
    private AccountService accountService;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
        accountService = new AccountService(accountRepository, clock);
    }

    @Test
    void createAccount_duplicateNumber_throwsConflict() {
        CreateAccountRequest request = new CreateAccountRequest(
                "ACC-DUP-1",
                10L,
                AccountType.SAVINGS,
                new BigDecimal("1000.00"),
                "BDT"
        );
        when(accountRepository.existsByAccountNo("ACC-DUP-1")).thenReturn(true);

        assertThatThrownBy(() -> accountService.createAccount(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void validate_frozenAccount_throws() {
        Account frozenAccount = new Account(
                "ACC-FROZEN",
                10L,
                AccountType.SAVINGS,
                new BigDecimal("5000.00"),
                AccountStatus.FROZEN,
                "BDT"
        );
        when(accountRepository.findByAccountNo("ACC-FROZEN")).thenReturn(Optional.of(frozenAccount));

        assertThatThrownBy(() -> accountService.validate("ACC-FROZEN"))
                .isInstanceOf(UnprocessableException.class)
                .hasMessageContaining("frozen");
    }

    @Test
    void debit_insufficientFunds_throws() {
        Account account = new Account(
                "ACC-1001",
                10L,
                AccountType.SAVINGS,
                new BigDecimal("500.00"),
                AccountStatus.ACTIVE,
                "BDT"
        );
        when(accountRepository.findByAccountNoForUpdate("ACC-1001")).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> accountService.debit("ACC-1001", new BigDecimal("1000.00"), "TXN-001"))
                .isInstanceOf(UnprocessableException.class)
                .hasMessageContaining("Insufficient funds");
    }

    @Test
    void credit_increasesBalance() {
        Account account = new Account(
                "ACC-1001",
                10L,
                AccountType.SAVINGS,
                new BigDecimal("500.00"),
                AccountStatus.ACTIVE,
                "BDT"
        );
        when(accountRepository.findByAccountNoForUpdate("ACC-1001")).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        accountService.credit("ACC-1001", new BigDecimal("250.00"), "TXN-002");

        assertThat(account.getBalance()).isEqualByComparingTo("750.00");
        verify(accountRepository).save(account);
    }

    @Test
    void createAccount_validRequest_createsAccount() {
        CreateAccountRequest request = new CreateAccountRequest(
                "ACC-NEW-1",
                20L,
                AccountType.CURRENT,
                new BigDecimal("2000.00"),
                "BDT"
        );
        when(accountRepository.existsByAccountNo("ACC-NEW-1")).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> {
            Account a = inv.getArgument(0);
            a.setId(99L);
            return a;
        });

        AccountResponse response = accountService.createAccount(request);

        assertThat(response.getId()).isEqualTo(99L);
        assertThat(response.getAccountNo()).isEqualTo("ACC-NEW-1");
        assertThat(response.getBalance()).isEqualByComparingTo("2000.00");
        assertThat(response.getStatus()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void validate_activeAccount_returnsTrue() {
        Account account = new Account(
                "ACC-ACTIVE",
                10L,
                AccountType.SAVINGS,
                new BigDecimal("1000.00"),
                AccountStatus.ACTIVE,
                "BDT"
        );
        when(accountRepository.findByAccountNo("ACC-ACTIVE")).thenReturn(Optional.of(account));

        boolean valid = accountService.validate("ACC-ACTIVE");

        assertThat(valid).isTrue();
    }

    @Test
    void validate_unknownAccount_returnsFalse() {
        when(accountRepository.findByAccountNo("ACC-UNKNOWN")).thenReturn(Optional.empty());

        boolean valid = accountService.validate("ACC-UNKNOWN");

        assertThat(valid).isFalse();
    }
}
