package com.aquariux.technical.assessment.trade.controller;

import com.aquariux.technical.assessment.trade.dto.request.TradeRequest;
import com.aquariux.technical.assessment.trade.dto.response.ErrorResponse;
import com.aquariux.technical.assessment.trade.dto.response.TradeResponse;
import com.aquariux.technical.assessment.trade.service.TradeServiceInterface;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/trades")
@Tag(name = "Trade", description = "Trading operations")
@RequiredArgsConstructor
public class TradeController {

    private final TradeServiceInterface tradeService;

    @PostMapping(value = "/execute", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Execute trade",
            description = "Execute a market BUY (at best ask) or SELL (at best bid) for a crypto pair using the user's wallet balance")
    @ApiResponse(responseCode = "201", description = "Trade executed")
    @ApiResponse(responseCode = "400", description = "Invalid request",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "User or trading pair not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "422", description = "Insufficient balance, or no valid/fresh market price",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<TradeResponse> executeTrade(@Valid @RequestBody TradeRequest tradeRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tradeService.executeTrade(tradeRequest));
    }
}
