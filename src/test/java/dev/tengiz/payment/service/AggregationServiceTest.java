package dev.tengiz.payment.service;

import dev.tengiz.payment.dto.request.TransactionRequest;
import dev.tengiz.payment.dto.response.BalanceResponse;
import dev.tengiz.payment.dto.response.TransactionResponse;
import dev.tengiz.payment.entity.DailyBalance;
import dev.tengiz.payment.exception.ResourceNotFoundException;
import dev.tengiz.payment.repository.DailyBalanceRepository;
import dev.tengiz.payment.repository.TransactionRepository;
import dev.tengiz.payment.service.impl.AggregationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AggregationServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private DailyBalanceRepository dailyBalanceRepository;

    @InjectMocks
    private AggregationServiceImpl aggregationService;

    private TransactionRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = TransactionRequest.builder()
            .transactionId(UUID.randomUUID().toString())
            .accountId("ACC-123")
            .amount(new BigDecimal("100.50"))
            .currency("USD")
            .timestamp(OffsetDateTime.now())
            .build();
    }

    @Test
    void processTransaction_NewTransaction_ReturnsCreated() {
        when(transactionRepository.processTransactionAtomically(
            any(), any(), any(), any(), any(), any()
        )).thenReturn(1);

        TransactionResponse response = aggregationService.processTransaction(validRequest);

        assertThat(response.getStatus()).isEqualTo(dev.tengiz.payment.dto.response.TransactionStatus.CREATED);
        assertThat(response.getTransactionId()).isEqualTo(validRequest.getTransactionId());
        verify(transactionRepository, times(1)).processTransactionAtomically(
            any(), any(), any(), any(), any(), any()
        );
    }

    @Test
    void processTransaction_DuplicateTransaction_ReturnsDuplicate() {
        when(transactionRepository.processTransactionAtomically(
            any(), any(), any(), any(), any(), any()
        )).thenReturn(0);

        TransactionResponse response = aggregationService.processTransaction(validRequest);

        assertThat(response.getStatus()).isEqualTo(dev.tengiz.payment.dto.response.TransactionStatus.DUPLICATE);
        assertThat(response.getMessage()).contains("already processed");
    }

    @Test
    void getBalance_ExistingBalance_ReturnsBalanceResponse() {
        LocalDate date = LocalDate.now();
        String accountId = "ACC-123";

        DailyBalance balance1 = DailyBalance.builder()
            .accountId(accountId)
            .currency("USD")
            .businessDate(date)
            .balance(new BigDecimal("1000.00"))
            .build();

        DailyBalance balance2 = DailyBalance.builder()
            .accountId(accountId)
            .currency("EUR")
            .businessDate(date)
            .balance(new BigDecimal("500.00"))
            .build();

        when(dailyBalanceRepository.findByAccountAndDate(accountId, date))
            .thenReturn(Arrays.asList(balance1, balance2));

        BalanceResponse response = aggregationService.getBalance(accountId, date);

        assertThat(response.getAccountId()).isEqualTo(accountId);
        assertThat(response.getDate()).isEqualTo(date);
        assertThat(response.getBalances()).hasSize(2);
        assertThat(response.getBalances().get(0).getCurrency()).isIn("USD", "EUR");
    }

    @Test
    void getBalance_NoBalance_ThrowsResourceNotFoundException() {
        LocalDate date = LocalDate.now();
        String accountId = "ACC-999";

        when(dailyBalanceRepository.findByAccountAndDate(accountId, date))
            .thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> aggregationService.getBalance(accountId, date))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("No balance found");
    }
}
