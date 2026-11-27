package com.switchpay.banking.service;

import com.switchpay.banking.dto.AccountResponse;
import com.switchpay.banking.dto.BalanceResponse;
import com.switchpay.banking.dto.CreateAccountRequest;
import com.switchpay.banking.dto.UpdateAccountRequest;
import com.switchpay.banking.entity.Account;
import com.switchpay.banking.enums.AccountStatus;
import com.switchpay.banking.repository.AccountRepository;
import com.switchpay.common.exception.BadRequestException;
import com.switchpay.common.exception.ConflictException;
import com.switchpay.common.exception.ForbiddenException;
import com.switchpay.common.exception.NotFoundException;
import com.switchpay.common.exception.UnprocessableException;
import com.switchpay.common.port.AccountPort;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService implements AccountPort {

    private final AccountRepository accountRepository;
    private final Clock clock;

    public AccountService(AccountRepository accountRepository, Clock clock) {
        this.accountRepository = accountRepository;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean validate(String accountNo) {
        if (accountNo == null || accountNo.isBlank()) {
            return false;
        }
        Account account = accountRepository.findByAccountNo(accountNo.trim())
                .orElse(null);
        if (account == null) {
            return false;
        }
        if (account.getStatus() == AccountStatus.FROZEN) {
            throw new UnprocessableException("Account is frozen: " + accountNo);
        }
        if (account.getStatus() == AccountStatus.CLOSED) {
            throw new UnprocessableException("Account is closed: " + accountNo);
        }
        return account.getStatus() == AccountStatus.ACTIVE;
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal balanceOf(String accountNo) {
        Account account = accountRepository.findByAccountNo(accountNo)
                .orElseThrow(() -> new NotFoundException("Account not found: " + accountNo));
        return account.getBalance();
    }

    @Override
    @Transactional
    public void debit(String accountNo, BigDecimal amount, String txnRef) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Debit amount must be positive");
        }
        Account account = accountRepository.findByAccountNoForUpdate(accountNo)
                .orElseGet(() -> accountRepository.findByAccountNo(accountNo)
                        .orElseThrow(() -> new NotFoundException("Account not found: " + accountNo)));

        if (account.getStatus() == AccountStatus.FROZEN) {
            throw new UnprocessableException("Account is frozen: " + accountNo);
        }
        if (account.getStatus() == AccountStatus.CLOSED) {
            throw new UnprocessableException("Account is closed: " + accountNo);
        }
        if (account.getBalance().compareTo(amount) < 0) {
            throw new UnprocessableException("Insufficient funds in account: " + accountNo);
        }

        account.setBalance(account.getBalance().subtract(amount));
        account.setUpdatedAt(Instant.now(clock));
        accountRepository.save(account);
    }

    @Override
    @Transactional
    public void credit(String accountNo, BigDecimal amount, String txnRef) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Credit amount must be positive");
        }
        Account account = accountRepository.findByAccountNoForUpdate(accountNo)
                .orElseGet(() -> accountRepository.findByAccountNo(accountNo)
                        .orElseThrow(() -> new NotFoundException("Account not found: " + accountNo)));

        if (account.getStatus() == AccountStatus.FROZEN) {
            throw new UnprocessableException("Account is frozen: " + accountNo);
        }
        if (account.getStatus() == AccountStatus.CLOSED) {
            throw new UnprocessableException("Account is closed: " + accountNo);
        }

        account.setBalance(account.getBalance().add(amount));
        account.setUpdatedAt(Instant.now(clock));
        accountRepository.save(account);
    }

    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {
        String accountNo = request.getAccountNo().trim();
        if (accountRepository.existsByAccountNo(accountNo)) {
            throw new ConflictException("Account number already exists: " + accountNo);
        }

        BigDecimal balance = request.getInitialBalance() != null ? request.getInitialBalance() : BigDecimal.ZERO;
        String currency = request.getCurrency() != null && !request.getCurrency().isBlank()
                ? request.getCurrency().trim().toUpperCase() : "BDT";

        Account account = new Account(accountNo, request.getUserId(), request.getType(), balance, AccountStatus.ACTIVE, currency);
        account.setCreatedAt(Instant.now(clock));
        account.setUpdatedAt(Instant.now(clock));
        Account saved = accountRepository.save(account);
        return AccountResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> listAccounts(Long userId) {
        List<Account> accounts;
        if (userId != null) {
            accounts = accountRepository.findByUserId(userId);
        } else {
            accounts = accountRepository.findAll();
        }
        return accounts.stream().map(AccountResponse::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccount(String accountNo, Long callerUserId, boolean isAdmin) {
        Account account = accountRepository.findByAccountNo(accountNo)
                .orElseThrow(() -> new NotFoundException("Account not found: " + accountNo));
        if (!isAdmin && (account.getUserId() == null || !account.getUserId().equals(callerUserId))) {
            throw new ForbiddenException("Not authorized to view account: " + accountNo);
        }
        return AccountResponse.fromEntity(account);
    }

    @Transactional
    public AccountResponse updateAccount(String accountNo, UpdateAccountRequest request) {
        Account account = accountRepository.findByAccountNo(accountNo)
                .orElseThrow(() -> new NotFoundException("Account not found: " + accountNo));

        if (request.getStatus() != null) {
            account.setStatus(request.getStatus());
        }
        if (request.getType() != null) {
            account.setType(request.getType());
        }
        account.setUpdatedAt(Instant.now(clock));
        Account saved = accountRepository.save(account);
        return AccountResponse.fromEntity(saved);
    }

    @Transactional
    public void closeAccount(String accountNo) {
        Account account = accountRepository.findByAccountNo(accountNo)
                .orElseThrow(() -> new NotFoundException("Account not found: " + accountNo));
        account.setStatus(AccountStatus.CLOSED);
        account.setUpdatedAt(Instant.now(clock));
        accountRepository.save(account);
    }

    @Transactional(readOnly = true)
    public BalanceResponse getBalance(String accountNo, Long callerUserId, boolean isAdmin) {
        Account account = accountRepository.findByAccountNo(accountNo)
                .orElseThrow(() -> new NotFoundException("Account not found: " + accountNo));
        if (!isAdmin && (account.getUserId() == null || !account.getUserId().equals(callerUserId))) {
            throw new ForbiddenException("Not authorized to view account balance: " + accountNo);
        }
        if (account.getStatus() == AccountStatus.CLOSED) {
            throw new UnprocessableException("Account is closed: " + accountNo);
        }
        if (account.getStatus() == AccountStatus.FROZEN) {
            throw new UnprocessableException("Account is frozen: " + accountNo);
        }
        return new BalanceResponse(account.getAccountNo(), account.getBalance(), account.getCurrency(), account.getStatus());
    }
}
