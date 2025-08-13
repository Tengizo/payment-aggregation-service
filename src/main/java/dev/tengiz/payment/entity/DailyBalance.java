package dev.tengiz.payment.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "daily_balance",schema = "payment")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(DailyBalance.DailyBalanceId.class)
public class DailyBalance {

    @Id
    @Column(name = "account_id", length = 64)
    private String accountId;

    @Id
    @Column(length = 3)
    private String currency;

    @Id
    @Column(name = "business_date")
    private LocalDate businessDate;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal balance;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyBalanceId implements Serializable {
        private String accountId;
        private String currency;
        private LocalDate businessDate;
    }
}
