package dev.tengiz.payment.mapper;

import dev.tengiz.payment.dto.CurrencyBalance;
import dev.tengiz.payment.dto.response.BalanceResponse;
import dev.tengiz.payment.entity.DailyBalance;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BalanceMapperTest {

    private final BalanceMapper mapper = new BalanceMapper();

    @Test
    void toCurrencyBalances_MapsEntitiesToDto() {
        LocalDate date = LocalDate.of(2025, 1, 1);
        DailyBalance usd = DailyBalance.builder()
            .accountId("A1")
            .currency("USD")
            .businessDate(date)
            .balance(new BigDecimal("100.00"))
            .build();
        DailyBalance eur = DailyBalance.builder()
            .accountId("A1")
            .currency("EUR")
            .businessDate(date)
            .balance(new BigDecimal("50.00"))
            .build();

        List<CurrencyBalance> items = mapper.toCurrencyBalances(Arrays.asList(usd, eur));
        assertThat(items).hasSize(2);
        assertThat(items).anySatisfy(i -> {
            assertThat(i.getCurrency()).isIn("USD", "EUR");
            assertThat(i.getBalance()).isIn(new BigDecimal("100.00"), new BigDecimal("50.00"));
        });
    }

    @Test
    void toBalanceResponse_BuildsResponse() {
        LocalDate date = LocalDate.of(2025, 1, 1);
        DailyBalance usd = DailyBalance.builder()
            .accountId("A1")
            .currency("USD")
            .businessDate(date)
            .balance(new BigDecimal("100.00"))
            .build();

        BalanceResponse resp = mapper.toBalanceResponse("A1", date, List.of(usd));
        assertThat(resp.getAccountId()).isEqualTo("A1");
        assertThat(resp.getDate()).isEqualTo(date);
        assertThat(resp.getBalances()).hasSize(1);
        assertThat(resp.getBalances().get(0).getCurrency()).isEqualTo("USD");
        assertThat(resp.getBalances().get(0).getBalance()).isEqualTo(new BigDecimal("100.00"));
    }
}
