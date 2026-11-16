package com.aquariux.technical.assessment.trade.service.impl;

import com.aquariux.technical.assessment.trade.config.TradingProperties;
import com.aquariux.technical.assessment.trade.dto.internal.MarketQuote;
import com.aquariux.technical.assessment.trade.dto.internal.TradingPairDto;
import com.aquariux.technical.assessment.trade.entity.CryptoPrice;
import com.aquariux.technical.assessment.trade.exception.InvalidMarketPriceException;
import com.aquariux.technical.assessment.trade.exception.PriceUnavailableException;
import com.aquariux.technical.assessment.trade.mapper.CryptoPriceMapper;
import com.aquariux.technical.assessment.trade.service.MarketPriceProviderInterface;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Clock;

/**
 * Supplies the latest aggregated best bid/ask stored by the price scheduler, after sanity checks:
 * the quote must exist, have positive prices and, when configured, not be older than
 * {@code trading.price-max-age}. Crossed quotes are uncrossed so the user always gets the worse price.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LatestMarketPriceProvider implements MarketPriceProviderInterface {

    private final CryptoPriceMapper cryptoPriceMapper;
    private final TradingProperties tradingProperties;
    private final Clock clock;

    @Override
    public MarketQuote getTradableQuote(TradingPairDto pair) {
        CryptoPrice price = cryptoPriceMapper.findLatestByPairId(pair.getId());
        if (price == null) {
            throw new PriceUnavailableException("No market price available for " + pair.getPairName());
        }

        validatePrices(pair, price);
        validateFreshness(pair, price);

        if (price.getBidPrice().compareTo(price.getAskPrice()) > 0) {
            return uncross(pair, price);
        }
        return new MarketQuote(price.getCryptoPairId(), price.getBidPrice(), price.getBidSource(),
                price.getAskPrice(), price.getAskSource(), price.getCreatedAt());
    }

    /**
     * A "crossed" quote (bid above ask) is normal for prices aggregated across exchanges: one exchange's
     * best bid can sit above the other's best ask. Trading at those prices as-is would let a user buy and
     * immediately sell back for a risk-free profit, so the user always gets the worse of the two prices:
     * buyers pay the higher one and sellers receive the lower one.
     */
    private MarketQuote uncross(TradingPairDto pair, CryptoPrice price) {
        log.debug("Crossed quote for {} (priceId={}): bid {} ({}) > ask {} ({}); using the worse price for each side",
                pair.getPairName(), price.getId(), price.getBidPrice(), price.getBidSource(),
                price.getAskPrice(), price.getAskSource());
        return new MarketQuote(price.getCryptoPairId(),
                price.getAskPrice(), price.getAskSource(),   // sellers receive the lower price
                price.getBidPrice(), price.getBidSource(),   // buyers pay the higher price
                price.getCreatedAt());
    }

    private void validatePrices(TradingPairDto pair, CryptoPrice price) {
        if (!isPositive(price.getBidPrice()) || !isPositive(price.getAskPrice())) {
            throw new InvalidMarketPriceException("Market price for " + pair.getPairName() + " is invalid");
        }
    }

    private void validateFreshness(TradingPairDto pair, CryptoPrice price) {
        if (!tradingProperties.isStalenessCheckEnabled()) {
            return;
        }
        if (price.getCreatedAt() == null) {
            throw new PriceUnavailableException("Market price for " + pair.getPairName() + " has no timestamp");
        }
        Duration age = Duration.between(price.getCreatedAt(), LocalDateTime.now(clock));
        if (age.compareTo(tradingProperties.getPriceMaxAge()) > 0) {
            throw new PriceUnavailableException("Market price for " + pair.getPairName()
                    + " is stale (" + age.toSeconds() + "s old), please retry shortly");
        }
    }

    private static boolean isPositive(BigDecimal value) {
        return value != null && value.signum() > 0;
    }
}
