package dev.tengiz.payment.repository;

import static org.assertj.core.api.Assertions.assertThat;

import dev.tengiz.payment.entity.DailyBalance;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@ActiveProfiles("test")
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TransactionRepositoryTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void registerDataSourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> true);
        registry.add("spring.flyway.locations", () -> "classpath:db/migration,classpath:db/testdata");
    }

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private DailyBalanceRepository dailyBalanceRepository;

    @Test
    void processTransactionAtomically_NewTransaction_ReturnsOne() {
        UUID transactionId = UUID.randomUUID();
        String accountId = "TEST-001";
        String currency = "USD";
        BigDecimal amount = new BigDecimal("100.00");
        // Fixed UTC timestamp for determinism
        OffsetDateTime timestamp = OffsetDateTime.parse("2025-01-10T10:15:30Z");
        LocalDate businessDate = timestamp.toLocalDate();

        int result = transactionRepository.processTransactionAtomically(
            transactionId, accountId, currency, amount, timestamp, businessDate
        );

        assertThat(result).isEqualTo(1);
        assertThat(transactionRepository.existsByTransactionId(transactionId)).isTrue();

        // Assert daily balance inserted/updated correctly
        List<DailyBalance> balances = dailyBalanceRepository.findByAccountAndDate(accountId, businessDate);
        assertThat(balances).isNotEmpty();
        DailyBalance usdBalance = balances.stream()
            .filter(b -> b.getCurrency().equals(currency))
            .findFirst()
            .orElse(null);
        assertThat(usdBalance).isNotNull();
        assertThat(usdBalance.getBalance()).isEqualByComparingTo(amount);
    }

    @Test
    void processTransactionAtomically_DuplicateTransaction_ReturnsZero() {
        UUID transactionId = UUID.randomUUID();
        String accountId = "TEST-002";
        String currency = "EUR";
        BigDecimal amount = new BigDecimal("50.00");
        // Fixed UTC timestamp for determinism
        OffsetDateTime timestamp = OffsetDateTime.parse("2025-01-11T08:00:00Z");
        LocalDate businessDate = timestamp.toLocalDate();

        int first = transactionRepository.processTransactionAtomically(
            transactionId, accountId, currency, amount, timestamp, businessDate
        );
        assertThat(first).isEqualTo(1);

        // Capture balance after first insert
        BigDecimal before = dailyBalanceRepository.findByAccountAndDate(accountId, businessDate).stream()
            .filter(b -> b.getCurrency().equals(currency))
            .map(DailyBalance::getBalance)
            .findFirst()
            .orElse(BigDecimal.ZERO);
        assertThat(before).isEqualByComparingTo(amount);

        int result = transactionRepository.processTransactionAtomically(
            transactionId, accountId, currency, amount, timestamp, businessDate
        );

        assertThat(result).isEqualTo(0);

        // Ensure balance unchanged after duplicate
        BigDecimal after = dailyBalanceRepository.findByAccountAndDate(accountId, businessDate).stream()
            .filter(b -> b.getCurrency().equals(currency))
            .map(DailyBalance::getBalance)
            .findFirst()
            .orElse(BigDecimal.ZERO);
        assertThat(after).isEqualByComparingTo(before);
    }
}
