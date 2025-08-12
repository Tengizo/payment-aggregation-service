package dev.tengiz.payment.repository;

import dev.tengiz.payment.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    /**
     * Atomically insert transaction and update daily balance using CTE.
     * Ensures idempotency (by transaction_id) and atomic balance increment.
     *
     * @return number of rows inserted into transactions (0 if duplicate, 1 if new)
     */
    @Modifying
    @Transactional
    @Query(value = """
        WITH ins AS (
            INSERT INTO transactions (
                transaction_id, account_id, currency, amount,
                ts_utc, business_date, created_at
            )
            VALUES (
                :transactionId, :accountId, :currency, :amount,
                :tsUtc, :businessDate, CURRENT_TIMESTAMP
            )
            ON CONFLICT (transaction_id) DO NOTHING
            RETURNING account_id, currency, amount, business_date
        )
        INSERT INTO daily_balance (account_id, currency, business_date, balance, updated_at)
        SELECT account_id, currency, business_date, amount, CURRENT_TIMESTAMP
        FROM ins
        ON CONFLICT (account_id, currency, business_date)
        DO UPDATE SET
            balance = daily_balance.balance + EXCLUDED.balance,
            updated_at = CURRENT_TIMESTAMP
        """, nativeQuery = true)
    int processTransactionAtomically(
        @Param("transactionId") UUID transactionId,
        @Param("accountId") String accountId,
        @Param("currency") String currency,
        @Param("amount") BigDecimal amount,
        @Param("tsUtc") OffsetDateTime tsUtc,
        @Param("businessDate") LocalDate businessDate
    );

    boolean existsByTransactionId(UUID transactionId);
}
