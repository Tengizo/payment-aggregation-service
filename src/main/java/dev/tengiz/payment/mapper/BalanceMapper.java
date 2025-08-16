package dev.tengiz.payment.mapper;

import dev.tengiz.payment.dto.CurrencyBalance;
import dev.tengiz.payment.dto.response.BalanceResponse;
import dev.tengiz.payment.entity.DailyBalance;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class BalanceMapper {

    public List<CurrencyBalance> toCurrencyBalances(List<DailyBalance> balances) {
        return balances.stream()
            .map(db -> CurrencyBalance.builder()
                .currency(db.getCurrency())
                .balance(db.getBalance())
                .build())
            .collect(Collectors.toList());
    }

    public BalanceResponse toBalanceResponse(String accountId, LocalDate date, List<DailyBalance> balances) {
        return BalanceResponse.builder()
            .accountId(accountId)
            .date(date)
            .balances(toCurrencyBalances(balances))
            .build();
    }
}
