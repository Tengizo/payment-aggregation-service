package dev.tengiz.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.tengiz.payment.dto.request.TransactionRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class TransactionControllerIntegrationTest {

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
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void processTransaction_ValidRequest_Returns201() throws Exception {
        TransactionRequest request = TransactionRequest.builder()
            .transactionId(UUID.randomUUID().toString())
            .accountId("ACC-100")
            .amount(new BigDecimal("250.75"))
            .currency("USD")
            .timestamp(OffsetDateTime.now())
            .build();

        mockMvc.perform(post("/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("CREATED"))
            .andExpect(jsonPath("$.transactionId").value(request.getTransactionId()));
    }

    @Test
    void processTransaction_DuplicateRequest_Returns200() throws Exception {
        String transactionId = UUID.randomUUID().toString();
        TransactionRequest request = TransactionRequest.builder()
            .transactionId(transactionId)
            .accountId("ACC-101")
            .amount(new BigDecimal("100.00"))
            .currency("EUR")
            .timestamp(OffsetDateTime.now())
            .build();

        mockMvc.perform(post("/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("DUPLICATE"));
    }

    @Test
    void testConcurrentTransactions_SameAccount_CorrectBalance() throws Exception {
        String accountId = "ACC-CONCURRENT";
        LocalDate today = LocalDate.now();
        int threadCount = 10;
        BigDecimal amountPerTransaction = new BigDecimal("100.00");

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                try {
                    startLatch.await();
                    TransactionRequest request = TransactionRequest.builder()
                        .transactionId(UUID.randomUUID().toString())
                        .accountId(accountId)
                        .amount(amountPerTransaction)
                        .currency("USD")
                        .timestamp(OffsetDateTime.now())
                        .build();

                    mockMvc.perform(post("/transactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isCreated());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }));
        }

        startLatch.countDown();
        for (Future<?> future : futures) {
            future.get();
        }

        BigDecimal expectedBalance = amountPerTransaction.multiply(new BigDecimal(threadCount));

        mockMvc.perform(get("/balances/{accountId}", accountId)
                .param("date", today.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accountId").value(accountId))
            .andExpect(jsonPath("$.balances[0].currency").value("USD"))
            .andExpect(jsonPath("$.balances[0].balance").value(expectedBalance.doubleValue()));
    }

    @Test
    void getBalance_ExistingBalance_Returns200() throws Exception {
        String accountId = "ACC-200";
        TransactionRequest request = TransactionRequest.builder()
            .transactionId(UUID.randomUUID().toString())
            .accountId(accountId)
            .amount(new BigDecimal("500.00"))
            .currency("GBP")
            .timestamp(OffsetDateTime.now())
            .build();

        mockMvc.perform(post("/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/balances/{accountId}", accountId)
                .param("date", LocalDate.now().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accountId").value(accountId))
            .andExpect(jsonPath("$.balances[0].currency").value("GBP"))
            .andExpect(jsonPath("$.balances[0].balance").value(500.00));
    }

    @Test
    void getBalance_NonExistentAccount_Returns404() throws Exception {
        mockMvc.perform(get("/balances/{accountId}", "NON-EXISTENT")
                .param("date", LocalDate.now().toString()))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }
    
    @Test
    void processTransaction_DuplicateWithDifferentAmount_Returns409() throws Exception {
        String transactionId = UUID.randomUUID().toString();
        TransactionRequest first = TransactionRequest.builder()
            .transactionId(transactionId)
            .accountId("ACC-409")
            .amount(new BigDecimal("123.45"))
            .currency("USD")
            .timestamp(OffsetDateTime.now())
            .build();

        TransactionRequest secondDifferent = TransactionRequest.builder()
            .transactionId(transactionId)
            .accountId("ACC-409")
            .amount(new BigDecimal("999.99"))
            .currency("USD")
            .timestamp(OffsetDateTime.now())
            .build();

        mockMvc.perform(post("/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(first)))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(secondDifferent)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value("CONFLICT"));
    }
}
