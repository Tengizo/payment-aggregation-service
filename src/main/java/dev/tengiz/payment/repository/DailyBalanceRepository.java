package dev.tengiz.payment.repository;

import dev.tengiz.payment.entity.DailyBalance;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DailyBalanceRepository extends JpaRepository<DailyBalance, DailyBalance.DailyBalanceId> {

    @Query("SELECT db FROM DailyBalance db WHERE db.accountId = :accountId AND db.businessDate = :date ORDER BY db.currency")
    List<DailyBalance> findByAccountAndDate(@Param("accountId") String accountId, @Param("date") LocalDate date);
}
