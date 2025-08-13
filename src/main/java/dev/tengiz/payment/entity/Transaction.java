package dev.tengiz.payment.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "transactions", schema = "payment")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class Transaction {

    @Id
    @Column(name = "transaction_id")
    private UUID transactionId;

    @Column(name = "account_id", nullable = false, length = 64)
    private String accountId;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "ts_utc", nullable = false)
    private OffsetDateTime tsUtc;

    @Column(name = "business_date", nullable = false)
    private LocalDate businessDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
        if (businessDate == null && tsUtc != null) {
            businessDate = tsUtc.toLocalDate();
        }
    }
}
