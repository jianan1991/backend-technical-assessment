package com.aquariux.technical.assessment.trade.service;

import com.aquariux.technical.assessment.trade.dto.internal.MarketQuote;
import com.aquariux.technical.assessment.trade.dto.internal.TradingPairDto;

public interface MarketPriceProviderInterface {

    /**
     * Returns the current tradable quote for a pair.
     *
     * @throws com.aquariux.technical.assessment.trade.exception.PriceUnavailableException   when no usable (or fresh enough) price exists
     * @throws com.aquariux.technical.assessment.trade.exception.InvalidMarketPriceException when the stored quote is not sane
     */
    MarketQuote getTradableQuote(TradingPairDto pair);
}
