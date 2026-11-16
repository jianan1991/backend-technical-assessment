package com.aquariux.technical.assessment.trade.service.impl;

import com.aquariux.technical.assessment.trade.config.TradingProperties;
import com.aquariux.technical.assessment.trade.dto.internal.MarketQuote;
import com.aquariux.technical.assessment.trade.dto.internal.TradingPairDto;
import com.aquariux.technical.assessment.trade.entity.CryptoPrice;
import com.aquariux.technical.assessment.trade.exception.InvalidMarketPriceException;
import com.aquariux.technical.assessment.trade.exception.PriceUnavailableException;
import com.aquariux.technical.assessment.trade.mapper.CryptoPriceMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LatestMarketPriceProviderTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2025-01-20T10:00:00Z"), ZoneOffset.UTC);
    private static final LocalDateTime NOW = LocalDateTime.now(CLOCK);

    @Mock
    private CryptoPriceMapper cryptoPriceMapper;

    private TradingProperties properties;
    private LatestMarketPriceProvider provider;
    private TradingPairDto pair;

    @BeforeEach
    void setUp() {
        properties = new TradingProperties();
        provider = new LatestMarketPriceProvider(cryptoPriceMapper, properties, CLOCK);
        pair = new TradingPairDto();
        pair.setId(1L);
        pair.setPairName("BTCUSDT");
    }

    private CryptoPrice price(String bid, String ask, LocalDateTime createdAt) {
        CryptoPrice price = new CryptoPrice();
        price.setId(10L);
        price.setCryptoPairId(1L);
        price.setBidPrice(bid == null ? null : new BigDecimal(bid));
        price.setAskPrice(ask == null ? null : new BigDecimal(ask));
        price.setBidSource("HUOBI");
        price.setAskSource("BINANCE");
        price.setCreatedAt(createdAt);
        return price;
    }

    @Test
    void getTradableQuote_ShouldReturnLatestValidQuote() {
        when(cryptoPriceMapper.findLatestByPairId(1L)).thenReturn(price("45000", "45010", NOW.minusDays(3)));

        MarketQuote quote = provider.getTradableQuote(pair);

        assertThat(quote.bidPrice()).isEqualByComparingTo("45000");
        assertThat(quote.askPrice()).isEqualByComparingTo("45010");
        assertThat(quote.bidSource()).isEqualTo("HUOBI");
        assertThat(quote.askSource()).isEqualTo("BINANCE");
    }

    @Test
    void getTradableQuote_WhenNoPrice_ShouldThrowUnavailable() {
        when(cryptoPriceMapper.findLatestByPairId(1L)).thenReturn(null);

        assertThatThrownBy(() -> provider.getTradableQuote(pair)).isInstanceOf(PriceUnavailableException.class);
    }

    @Test
    void getTradableQuote_WhenCrossed_ShouldGiveUserTheWorsePriceOnEachSide() {
        // bid 64624.10 on HUOBI is above ask 64620.92 on BINANCE
        when(cryptoPriceMapper.findLatestByPairId(1L)).thenReturn(price("64624.10", "64620.92", NOW));

        MarketQuote quote = provider.getTradableQuote(pair);

        // buyers pay the higher price, sellers receive the lower one, each with its own exchange
        assertThat(quote.askPrice()).isEqualByComparingTo("64624.10");
        assertThat(quote.askSource()).isEqualTo("HUOBI");
        assertThat(quote.bidPrice()).isEqualByComparingTo("64620.92");
        assertThat(quote.bidSource()).isEqualTo("BINANCE");
        assertThat(quote.askPrice()).isGreaterThanOrEqualTo(quote.bidPrice());
    }

    @Test
    void getTradableQuote_WhenNonPositive_ShouldThrowInvalid() {
        when(cryptoPriceMapper.findLatestByPairId(1L)).thenReturn(price("0", "10", NOW));

        assertThatThrownBy(() -> provider.getTradableQuote(pair)).isInstanceOf(InvalidMarketPriceException.class);
    }

    @Test
    void getTradableQuote_WhenStalenessEnabledAndTooOld_ShouldThrowUnavailable() {
        properties.setPriceMaxAge(Duration.ofSeconds(30));
        when(cryptoPriceMapper.findLatestByPairId(1L)).thenReturn(price("45000", "45010", NOW.minusSeconds(31)));

        assertThatThrownBy(() -> provider.getTradableQuote(pair))
                .isInstanceOf(PriceUnavailableException.class)
                .hasMessageContaining("stale");
    }

    @Test
    void getTradableQuote_WhenStalenessEnabledAndFresh_ShouldReturnQuote() {
        properties.setPriceMaxAge(Duration.ofSeconds(30));
        when(cryptoPriceMapper.findLatestByPairId(1L)).thenReturn(price("45000", "45010", NOW.minusSeconds(29)));

        assertThat(provider.getTradableQuote(pair).askPrice()).isEqualByComparingTo("45010");
    }
}
