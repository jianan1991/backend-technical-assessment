package com.aquariux.technical.assessment.trade.service.strategy;

import com.aquariux.technical.assessment.trade.dto.internal.MarketQuote;
import com.aquariux.technical.assessment.trade.dto.internal.TradeSettlement;
import com.aquariux.technical.assessment.trade.dto.internal.TradingPairDto;
import com.aquariux.technical.assessment.trade.enums.TradeType;

import java.math.BigDecimal;

/**
 * Decides, for one trade type, which side of the quote is used and which wallets move.
 * New trade types are added by writing a new implementation (Open/Closed principle).
 */
public interface TradeExecutionStrategy {

    TradeType getTradeType();

    TradeSettlement settle(TradingPairDto pair, MarketQuote quote, BigDecimal quantity);
}
