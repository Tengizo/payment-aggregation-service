package dev.tengiz.payment.service.impl;

import dev.tengiz.payment.dto.request.TransactionRequest;
import dev.tengiz.payment.dto.response.BalanceResponse;
import dev.tengiz.payment.dto.response.TransactionResponse;
import dev.tengiz.payment.entity.DailyBalance;
import dev.tengiz.payment.exception.ConflictException;
import dev.tengiz.payment.exception.ResourceNotFoundException;
import dev.tengiz.payment.mapper.BalanceMapper;
import dev.tengiz.payment.mapper.TransactionMapper;
import dev.tengiz.payment.repository.DailyBalanceRepository;
import dev.tengiz.payment.repository.TransactionRepository;
import dev.tengiz.payment.service.AggregationService;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class AggregationServiceImpl implements AggregationService {

    private final TransactionRepository transactionRepository;
    private final DailyBalanceRepository dailyBalanceRepository;
    private final TransactionMapper transactionMapper;
    private final BalanceMapper balanceMapper;

    @Override
    @Transactional
    public TransactionResponse processTransaction(TransactionRequest request) {
        log.debug("Processing transaction: {}", request.getTransactionId());

        UUID txId = UUID.fromString(request.getTransactionId());

        // Compute business date from UTC timestamp using mapper
        LocalDate businessDate = transactionMapper.toBusinessDate(request.getTimestamp());

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
            return transactionMapper.toCreatedResponse(request);
        } else {
            // Duplicate: verify idempotency by comparing amount with existing transaction
            validateDuplicateAmount(txId, request);
            log.info("Duplicate transaction detected: {}", txId);
            return transactionMapper.toDuplicateResponse(request);
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

        log.info("Retrieved {} currency balances for account {}", balances.size(), accountId);

        return balanceMapper.toBalanceResponse(accountId, date, balances);
    }

    private void validateDuplicateAmount(UUID txId, TransactionRequest request) {
        transactionRepository.findById(txId).ifPresent(existing -> {
            if (existing.getAmount() != null && request.getAmount() != null) {
                if (existing.getAmount().compareTo(request.getAmount()) != 0) {
                    throw new ConflictException(String.format(
                        "Transaction %s already exists with a different amount. Existing=%s, Provided=%s",
                        txId, existing.getAmount(), request.getAmount()));
                }
            }
        });
    }
}
