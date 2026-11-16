package com.aquariux.technical.assessment.trade.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Turns on {@code @Scheduled} jobs (the exchange price poller).
 * <p>
 * Previously {@code @EnableScheduling} was imported in {@link SchedulerConfig} but never applied,
 * so {@code PriceUpdateScheduler} never ran. It can be switched off with
 * {@code trading.scheduler.enabled=false} (done in tests so they never call external exchanges).
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(prefix = "trading.scheduler", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingEnablementConfig {
}
