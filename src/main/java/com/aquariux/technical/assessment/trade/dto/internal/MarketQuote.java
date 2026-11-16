package com.aquariux.technical.assessment.trade.dto.internal;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A validated best bid/ask snapshot for one trading pair.
 */
public record MarketQuote(
        Long cryptoPairId,
        BigDecimal bidPrice,
        String bidSource,
        BigDecimal askPrice,
        String askSource,
        LocalDateTime quotedAt) {
}
