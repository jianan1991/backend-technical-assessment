package com.aquariux.technical.assessment.trade.service.strategy;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Monetary arithmetic shared by trade strategies. Amounts are stored as DECIMAL(20,8).
 */
public final class TradeAmounts {

    public static final int SCALE = 8;

    private TradeAmounts() {
    }

    /**
     * quantity x price at storage scale. Rounding is chosen by the caller so the platform never
     * gives away value: amounts the user pays round UP, amounts the user receives round DOWN.
     */
    public static BigDecimal notional(BigDecimal quantity, BigDecimal price, RoundingMode roundingMode) {
        return quantity.multiply(price).setScale(SCALE, roundingMode);
    }
}
