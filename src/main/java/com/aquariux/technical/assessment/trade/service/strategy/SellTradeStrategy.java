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
 * SELL: the user gives up the base asset at the best bid and receives the quote asset.
 * Proceeds are rounded down so the user is never credited more than the exact notional.
 */
@Component
public class SellTradeStrategy implements TradeExecutionStrategy {

    @Override
    public TradeType getTradeType() {
        return TradeType.SELL;
    }

    @Override
    public TradeSettlement settle(TradingPairDto pair, MarketQuote quote, BigDecimal quantity) {
        BigDecimal price = quote.bidPrice();
        BigDecimal proceeds = TradeAmounts.notional(quantity, price, RoundingMode.DOWN);
        return new TradeSettlement(
                price,
                quote.bidSource(),
                proceeds,
                new WalletMovement(pair.getBaseSymbolId(), pair.getBaseSymbol(), quantity),
                new WalletMovement(pair.getQuoteSymbolId(), pair.getQuoteSymbol(), proceeds));
    }
}
