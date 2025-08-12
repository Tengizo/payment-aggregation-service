package dev.tengiz.payment.service;

import dev.tengiz.payment.dto.request.TransactionRequest;
import dev.tengiz.payment.dto.response.BalanceResponse;
import dev.tengiz.payment.dto.response.TransactionResponse;

import java.time.LocalDate;

public interface AggregationService {

    /**
     * Process a transaction atomically, updating daily balance
     * @param request transaction details
     * @return response indicating success or duplicate
     */
    TransactionResponse processTransaction(TransactionRequest request);

    /**
     * Retrieve daily balances for an account
     * @param accountId account identifier
     * @param date business date (UTC)
     * @return balance response with all currencies
     */
    BalanceResponse getBalance(String accountId, LocalDate date);
}
