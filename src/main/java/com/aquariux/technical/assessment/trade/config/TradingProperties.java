package com.aquariux.technical.assessment.trade.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Externalised settings for the trading engine (bound from the {@code trading.*} properties).
 */
@Data
@ConfigurationProperties(prefix = "trading")
public class TradingProperties {

    /**
     * Maximum age of a quote that a trade may execute against.
     * {@code null} or zero disables the staleness check.
     */
    private Duration priceMaxAge;

    public boolean isStalenessCheckEnabled() {
        return priceMaxAge != null && !priceMaxAge.isZero() && !priceMaxAge.isNegative();
    }
}
