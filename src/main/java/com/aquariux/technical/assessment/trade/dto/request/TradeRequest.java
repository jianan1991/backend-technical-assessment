package com.aquariux.technical.assessment.trade.dto.request;

import com.aquariux.technical.assessment.trade.enums.TradeType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TradeRequest {

    @Schema(description = "Pre-authenticated user id", example = "1")
    @NotNull(message = "userId is required")
    @Positive(message = "userId must be positive")
    private Long userId;

    @Schema(description = "BUY executes at the best ask, SELL at the best bid", example = "BUY")
    @NotNull(message = "tradeType is required (BUY or SELL)")
    private TradeType tradeType;

    @Schema(description = "Trading pair", example = "ETHUSDT")
    @NotBlank(message = "pairName is required")
    @Size(max = 20, message = "pairName must be at most 20 characters")
    private String pairName;

    @Schema(description = "Amount of the base asset to buy or sell (e.g. 0.5 ETH)", example = "0.5")
    @NotNull(message = "quantity is required")
    @DecimalMin(value = "0", inclusive = false, message = "quantity must be greater than 0")
    @Digits(integer = 12, fraction = 8, message = "quantity supports at most 12 integer and 8 decimal digits")
    private BigDecimal quantity;
}
