package com.aquariux.technical.assessment.trade.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties(TradingProperties.class)
public class TradingConfig {

    /**
     * Single source of time for the trading engine, injected so tests can use a fixed clock.
     */
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
