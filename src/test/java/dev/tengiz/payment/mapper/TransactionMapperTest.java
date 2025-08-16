package dev.tengiz.payment.mapper;

import dev.tengiz.payment.dto.request.TransactionRequest;
import dev.tengiz.payment.dto.response.TransactionResponse;
import dev.tengiz.payment.dto.response.TransactionStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionMapperTest {

    private final TransactionMapper mapper = new TransactionMapper();

    @Test
    void toBusinessDate_ConvertsToUtcLocalDate() {
        // 2025-01-01T23:30-02:00 -> UTC date 2025-01-02
        OffsetDateTime odt = OffsetDateTime.of(2025, 1, 1, 23, 30, 0, 0, ZoneOffset.ofHours(-2));
        assertThat(mapper.toBusinessDate(odt)).isEqualTo(LocalDate.of(2025, 1, 2));

        // 2025-01-02T01:00+03:00 -> UTC date 2025-01-01 22:00, so 2025-01-01
        OffsetDateTime odt2 = OffsetDateTime.of(2025, 1, 2, 1, 0, 0, 0, ZoneOffset.ofHours(3));
        assertThat(mapper.toBusinessDate(odt2)).isEqualTo(LocalDate.of(2025, 1, 1));
    }

    @Test
    void toBusinessDate_NullTimestamp_ReturnsNull() {
        assertThat(mapper.toBusinessDate(null)).isNull();
    }

    @Test
    void toCreatedResponse_BuildsExpectedResponse() {
        TransactionRequest req = TransactionRequest.builder()
            .transactionId(UUID.randomUUID().toString())
            .accountId("ACC-1")
            .amount(new BigDecimal("10.00"))
            .currency("USD")
            .timestamp(OffsetDateTime.now())
            .build();

        TransactionResponse resp = mapper.toCreatedResponse(req);
        assertThat(resp.getTransactionId()).isEqualTo(req.getTransactionId());
        assertThat(resp.getStatus()).isEqualTo(TransactionStatus.CREATED);
        assertThat(resp.getMessage()).contains("processed");
    }

    @Test
    void toDuplicateResponse_BuildsExpectedResponse() {
        TransactionRequest req = TransactionRequest.builder()
            .transactionId(UUID.randomUUID().toString())
            .accountId("ACC-1")
            .amount(new BigDecimal("10.00"))
            .currency("USD")
            .timestamp(OffsetDateTime.now())
            .build();

        TransactionResponse resp = mapper.toDuplicateResponse(req);
        assertThat(resp.getTransactionId()).isEqualTo(req.getTransactionId());
        assertThat(resp.getStatus()).isEqualTo(TransactionStatus.DUPLICATE);
        assertThat(resp.getMessage()).contains("already processed");
    }
}
