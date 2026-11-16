package com.aquariux.technical.assessment.trade.dto.response;

import com.aquariux.technical.assessment.trade.enums.TradeType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TradeResponse {
    private Long tradeId;
    private Long userId;
    private String pairName;
    private TradeType tradeType;
    /** Base asset quantity traded. */
    private BigDecimal quantity;
    /** Execution price in the quote asset. */
    private BigDecimal price;
    /** quantity x price in the quote asset (debited for BUY, credited for SELL). */
    private BigDecimal totalAmount;
    /** Exchange the execution price came from (BINANCE / HUOBI). */
    private String priceSource;
    private LocalDateTime tradeTime;
}
