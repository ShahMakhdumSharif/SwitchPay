package com.switchpay.banking.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.switchpay.common.dto.HandlerResult;
import com.switchpay.common.dto.TransactionRequest;
import com.switchpay.common.enums.TransactionStatus;
import com.switchpay.common.enums.TransactionType;
import com.switchpay.common.exception.UnprocessableException;
import com.switchpay.common.port.AccountPort;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BankingTransactionHandlerTest {

    @Mock
    private AccountPort accountPort;

    private BankingTransactionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new BankingTransactionHandler(accountPort);
    }

    @Test
    void supports_bankingTypes_returnsTrue() {
        assertThat(handler.supports(TransactionType.ACCOUNT_VALIDATION)).isTrue();
        assertThat(handler.supports(TransactionType.BALANCE_INQUIRY)).isTrue();
        assertThat(handler.supports(TransactionType.CASH_WITHDRAWAL)).isTrue();
        assertThat(handler.supports(TransactionType.TRANSFER)).isTrue();
        assertThat(handler.supports(TransactionType.MERCHANT_PAYMENT)).isFalse();
    }

    @Test
    void handleAccountValidation_whenValid_returnsApproved() {
        TransactionRequest request = TransactionRequest.builder()
                .type(TransactionType.ACCOUNT_VALIDATION)
                .sourceAccountNo("ACC-1001")
                .build();
        when(accountPort.validate("ACC-1001")).thenReturn(true);

        HandlerResult result = handler.handle(request, "TXN-VAL-1");

        assertThat(result.getStatus()).isEqualTo(TransactionStatus.APPROVED);
        assertThat(result.isApproved()).isTrue();
    }

    @Test
    void handleAccountValidation_whenInvalid_returnsDeclined() {
        TransactionRequest request = TransactionRequest.builder()
                .type(TransactionType.ACCOUNT_VALIDATION)
                .sourceAccountNo("ACC-UNKNOWN")
                .build();
        when(accountPort.validate("ACC-UNKNOWN")).thenReturn(false);

        HandlerResult result = handler.handle(request, "TXN-VAL-2");

        assertThat(result.getStatus()).isEqualTo(TransactionStatus.DECLINED);
        assertThat(result.getDeclineReason()).containsIgnoringCase("invalid or inactive");
    }

    @Test
    void handleBalanceInquiry_whenValid_returnsApprovedWithBalance() {
        TransactionRequest request = TransactionRequest.builder()
                .type(TransactionType.BALANCE_INQUIRY)
                .sourceAccountNo("ACC-1001")
                .build();
        when(accountPort.validate("ACC-1001")).thenReturn(true);
        when(accountPort.balanceOf("ACC-1001")).thenReturn(new BigDecimal("15000.00"));

        HandlerResult result = handler.handle(request, "TXN-BAL-1");

        assertThat(result.getStatus()).isEqualTo(TransactionStatus.APPROVED);
        assertThat(result.getBalance()).isEqualByComparingTo("15000.00");
    }

    @Test
    void handleCashWithdrawal_whenValid_debitsAccountAndReturnsApproved() {
        TransactionRequest request = TransactionRequest.builder()
                .type(TransactionType.CASH_WITHDRAWAL)
                .sourceAccountNo("ACC-1001")
                .amount(new BigDecimal("1000.00"))
                .build();
        when(accountPort.validate("ACC-1001")).thenReturn(true);
        when(accountPort.balanceOf("ACC-1001")).thenReturn(new BigDecimal("9000.00"));

        HandlerResult result = handler.handle(request, "TXN-WDL-1");

        assertThat(result.getStatus()).isEqualTo(TransactionStatus.APPROVED);
        assertThat(result.getBalance()).isEqualByComparingTo("9000.00");
        verify(accountPort).debit("ACC-1001", new BigDecimal("1000.00"), "TXN-WDL-1");
    }

    @Test
    void handleCashWithdrawal_whenInsufficientFunds_returnsDeclined() {
        TransactionRequest request = TransactionRequest.builder()
                .type(TransactionType.CASH_WITHDRAWAL)
                .sourceAccountNo("ACC-1001")
                .amount(new BigDecimal("5000.00"))
                .build();
        when(accountPort.validate("ACC-1001")).thenReturn(true);
        doThrow(new UnprocessableException("Insufficient funds in account: ACC-1001"))
                .when(accountPort).debit("ACC-1001", new BigDecimal("5000.00"), "TXN-WDL-2");

        HandlerResult result = handler.handle(request, "TXN-WDL-2");

        assertThat(result.getStatus()).isEqualTo(TransactionStatus.DECLINED);
        assertThat(result.getDeclineReason()).containsIgnoringCase("Insufficient funds");
    }

    @Test
    void handleTransfer_whenValid_debitsSourceAndCreditsDestination() {
        TransactionRequest request = TransactionRequest.builder()
                .type(TransactionType.TRANSFER)
                .sourceAccountNo("ACC-1001")
                .destinationAccountNo("ACC-2001")
                .amount(new BigDecimal("2000.00"))
                .build();
        when(accountPort.validate("ACC-1001")).thenReturn(true);
        when(accountPort.validate("ACC-2001")).thenReturn(true);
        when(accountPort.balanceOf("ACC-1001")).thenReturn(new BigDecimal("8000.00"));

        HandlerResult result = handler.handle(request, "TXN-TRF-1");

        assertThat(result.getStatus()).isEqualTo(TransactionStatus.APPROVED);
        assertThat(result.getBalance()).isEqualByComparingTo("8000.00");
        verify(accountPort).debit("ACC-1001", new BigDecimal("2000.00"), "TXN-TRF-1");
        verify(accountPort).credit("ACC-2001", new BigDecimal("2000.00"), "TXN-TRF-1");
    }

    @Test
    void handleTransfer_whenSameAccount_returnsDeclined() {
        TransactionRequest request = TransactionRequest.builder()
                .type(TransactionType.TRANSFER)
                .sourceAccountNo("ACC-1001")
                .destinationAccountNo("ACC-1001")
                .amount(new BigDecimal("500.00"))
                .build();

        HandlerResult result = handler.handle(request, "TXN-TRF-2");

        assertThat(result.getStatus()).isEqualTo(TransactionStatus.DECLINED);
        assertThat(result.getDeclineReason()).containsIgnoringCase("must be different");
    }
}
