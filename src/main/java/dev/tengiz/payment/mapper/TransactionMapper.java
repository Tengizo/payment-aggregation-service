package dev.tengiz.payment.mapper;

import dev.tengiz.payment.dto.request.TransactionRequest;
import dev.tengiz.payment.dto.response.TransactionResponse;
import dev.tengiz.payment.dto.response.TransactionStatus;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.stereotype.Component;

@Component
public class TransactionMapper {

    public LocalDate toBusinessDate(OffsetDateTime timestamp) {
        if (timestamp == null) {
            return null;
        }
        return timestamp.atZoneSameInstant(ZoneOffset.UTC).toLocalDate();
    }

    public TransactionResponse toCreatedResponse(TransactionRequest request) {
        return TransactionResponse.builder()
            .transactionId(request.getTransactionId())
            .status(TransactionStatus.CREATED)
            .message("Transaction processed successfully")
            .build();
    }

    public TransactionResponse toDuplicateResponse(TransactionRequest request) {
        return TransactionResponse.builder()
            .transactionId(request.getTransactionId())
            .status(TransactionStatus.DUPLICATE)
            .message("Transaction already processed")
            .build();
    }
}
