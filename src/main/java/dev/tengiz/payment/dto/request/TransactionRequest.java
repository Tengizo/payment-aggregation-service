package dev.tengiz.payment.dto.request;

import dev.tengiz.payment.validation.ValidCurrency;
import dev.tengiz.payment.validation.ValidUUID;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import javax.validation.constraints.Digits;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionRequest {

    @NotBlank(message = "Transaction ID is required")
    @ValidUUID(message = "Transaction ID must be a valid UUID")
    private String transactionId;

    @NotBlank(message = "Account ID is required")
    @Size(min = 1, max = 64, message = "Account ID must be between 1 and 64 characters")
    @Pattern(regexp = "^[A-Za-z0-9-_]+$",
             message = "Account ID can only contain letters, numbers, hyphens and underscores")
    private String accountId;

    @NotNull(message = "Amount is required")
    @Digits(integer = 15, fraction = 4, message = "Amount must have max 15 integer and 4 decimal digits")
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    @ValidCurrency(message = "Currency must be a valid ISO 4217 code")
    private String currency;

    @NotNull(message = "Timestamp is required")
    private OffsetDateTime timestamp;
}
