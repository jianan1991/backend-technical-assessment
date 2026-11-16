package com.aquariux.technical.assessment.trade.service.strategy;

import com.aquariux.technical.assessment.trade.enums.TradeType;
import com.aquariux.technical.assessment.trade.exception.UnsupportedTradeTypeException;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Looks up the strategy for a trade type. Every {@link TradeExecutionStrategy} bean is registered
 * automatically, so adding a trade type never requires changing this class or the trade service.
 */
@Component
public class TradeExecutionStrategyResolver {

    private final Map<TradeType, TradeExecutionStrategy> strategies = new EnumMap<>(TradeType.class);

    public TradeExecutionStrategyResolver(List<TradeExecutionStrategy> strategyBeans) {
        for (TradeExecutionStrategy strategy : strategyBeans) {
            TradeExecutionStrategy previous = strategies.putIfAbsent(strategy.getTradeType(), strategy);
            if (previous != null) {
                throw new IllegalStateException("Duplicate strategy for trade type " + strategy.getTradeType());
            }
        }
    }

    public TradeExecutionStrategy resolve(TradeType tradeType) {
        TradeExecutionStrategy strategy = tradeType == null ? null : strategies.get(tradeType);
        if (strategy == null) {
            throw new UnsupportedTradeTypeException(tradeType);
        }
        return strategy;
    }
}
