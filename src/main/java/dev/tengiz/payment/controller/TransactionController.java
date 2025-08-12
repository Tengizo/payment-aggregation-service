package dev.tengiz.payment.controller;

import dev.tengiz.payment.dto.request.TransactionRequest;
import dev.tengiz.payment.dto.response.TransactionResponse;
import dev.tengiz.payment.dto.response.TransactionStatus;
import dev.tengiz.payment.service.AggregationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "Transactions", description = "Transaction processing endpoints")
public class TransactionController {

    private final AggregationService aggregationService;

    @PostMapping
    @Operation(
        summary = "Process a new transaction",
        description = "Atomically process a transaction and update daily balance"
    )
    @ApiResponses(
        value = {
            @ApiResponse(responseCode = "201", description = "Transaction created successfully"),
            @ApiResponse(responseCode = "200", description = "Duplicate transaction (idempotent)"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
        }
    )
    public ResponseEntity<TransactionResponse> processTransaction(
        @Valid @RequestBody TransactionRequest request
    ) {
        log.info("Received transaction request: transactionId={}, accountId={}, amount={}, currency={}",
            request.getTransactionId(), request.getAccountId(),
            request.getAmount(), request.getCurrency());

        TransactionResponse response = aggregationService.processTransaction(request);
        HttpStatus status = response.getStatus() == TransactionStatus.CREATED ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(response);
    }
}
