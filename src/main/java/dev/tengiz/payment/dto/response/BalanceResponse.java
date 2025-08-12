package dev.tengiz.payment.dto.response;

import dev.tengiz.payment.dto.CurrencyBalance;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BalanceResponse {
    private String accountId;
    private LocalDate date;
    private List<CurrencyBalance> balances;
}
