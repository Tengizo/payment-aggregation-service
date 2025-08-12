package dev.tengiz.payment.service.impl;

import dev.tengiz.payment.dto.CurrencyBalance;
import dev.tengiz.payment.dto.request.TransactionRequest;
import dev.tengiz.payment.dto.response.BalanceResponse;
import dev.tengiz.payment.dto.response.TransactionResponse;
import dev.tengiz.payment.dto.response.TransactionStatus;
import dev.tengiz.payment.entity.DailyBalance;
import dev.tengiz.payment.exception.ResourceNotFoundException;
import dev.tengiz.payment.repository.DailyBalanceRepository;
import dev.tengiz.payment.repository.TransactionRepository;
import dev.tengiz.payment.service.AggregationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AggregationServiceImpl implements AggregationService {

    private final TransactionRepository transactionRepository;
    private final DailyBalanceRepository dailyBalanceRepository;

    @Override
    @Transactional
    public TransactionResponse processTransaction(TransactionRequest request) {
        log.debug("Processing transaction: {}", request.getTransactionId());

        UUID txId = UUID.fromString(request.getTransactionId());

        // Compute business date from UTC timestamp
        LocalDate businessDate = request.getTimestamp()
            .atZoneSameInstant(ZoneOffset.UTC)
            .toLocalDate();

        // Execute atomic CTE operation
        int rowsAffected = transactionRepository.processTransactionAtomically(
            txId,
            request.getAccountId(),
            request.getCurrency().toUpperCase(),
            request.getAmount(),
            request.getTimestamp(),
            businessDate
        );

        if (rowsAffected > 0) {
            log.info("Transaction {} processed successfully for account {}", txId, request.getAccountId());
            return TransactionResponse.builder()
                .transactionId(request.getTransactionId())
                .status(TransactionStatus.CREATED)
                .message("Transaction processed successfully")
                .build();
        } else {
            log.info("Duplicate transaction detected: {}", txId);
            return TransactionResponse.builder()
                .transactionId(request.getTransactionId())
                .status(TransactionStatus.DUPLICATE)
                .message("Transaction already processed")
                .build();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public BalanceResponse getBalance(String accountId, LocalDate date) {
        log.debug("Retrieving balance for account {} on date {}", accountId, date);

        List<DailyBalance> balances = dailyBalanceRepository.findByAccountAndDate(accountId, date);

        if (balances.isEmpty()) {
            log.warn("No balance found for account {} on date {}", accountId, date);
            throw new ResourceNotFoundException(
                String.format("No balance found for account %s on date %s", accountId, date)
            );
        }

        List<CurrencyBalance> currencyBalances = balances.stream()
            .map(db -> CurrencyBalance.builder()
                .currency(db.getCurrency())
                .balance(db.getBalance())
                .build())
            .collect(Collectors.toList());

        log.info("Retrieved {} currency balances for account {}", currencyBalances.size(), accountId);

        return BalanceResponse.builder()
            .accountId(accountId)
            .date(date)
            .balances(currencyBalances)
            .build();
    }
}
