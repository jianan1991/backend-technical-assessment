package com.aquariux.technical.assessment.trade.service.strategy;

import com.aquariux.technical.assessment.trade.dto.internal.MarketQuote;
import com.aquariux.technical.assessment.trade.dto.internal.TradeSettlement;
import com.aquariux.technical.assessment.trade.dto.internal.TradeSettlement.WalletMovement;
import com.aquariux.technical.assessment.trade.dto.internal.TradingPairDto;
import com.aquariux.technical.assessment.trade.enums.TradeType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * BUY: the user pays the quote asset at the best ask and receives the base asset.
 * Cost is rounded up so the user always pays at least the exact notional.
 */
@Component
public class BuyTradeStrategy implements TradeExecutionStrategy {

    @Override
    public TradeType getTradeType() {
        return TradeType.BUY;
    }

    @Override
    public TradeSettlement settle(TradingPairDto pair, MarketQuote quote, BigDecimal quantity) {
        BigDecimal price = quote.askPrice();
        BigDecimal cost = TradeAmounts.notional(quantity, price, RoundingMode.UP);
        return new TradeSettlement(
                price,
                quote.askSource(),
                cost,
                new WalletMovement(pair.getQuoteSymbolId(), pair.getQuoteSymbol(), cost),
                new WalletMovement(pair.getBaseSymbolId(), pair.getBaseSymbol(), quantity));
    }
}
