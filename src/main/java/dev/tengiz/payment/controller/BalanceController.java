package dev.tengiz.payment.controller;

import dev.tengiz.payment.dto.response.BalanceResponse;
import dev.tengiz.payment.service.AggregationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;

@RestController
@RequestMapping("/balances")
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "Balances", description = "Balance retrieval endpoints")
public class BalanceController {

    private final AggregationService aggregationService;

    @GetMapping("/{accountId}")
    @Operation(
        summary = "Get daily balance for an account",
        description = "Retrieve all currency balances for a specific date"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Balance retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "No balance found for account/date"),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters")
    })
    public ResponseEntity<BalanceResponse> getBalance(
        @Parameter(description = "Account identifier", required = true)
        @PathVariable @NotBlank String accountId,
        @Parameter(description = "Date in YYYY-MM-DD format", required = true)
        @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        log.info("Retrieving balance for account: {} on date: {}", accountId, date);
        BalanceResponse response = aggregationService.getBalance(accountId, date);
        return ResponseEntity.ok(response);
    }
}
