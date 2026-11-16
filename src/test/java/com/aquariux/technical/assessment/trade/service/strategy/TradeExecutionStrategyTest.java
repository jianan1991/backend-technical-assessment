package com.aquariux.technical.assessment.trade.service.strategy;

import com.aquariux.technical.assessment.trade.dto.internal.MarketQuote;
import com.aquariux.technical.assessment.trade.dto.internal.TradeSettlement;
import com.aquariux.technical.assessment.trade.dto.internal.TradingPairDto;
import com.aquariux.technical.assessment.trade.enums.TradeType;
import com.aquariux.technical.assessment.trade.exception.UnsupportedTradeTypeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TradeExecutionStrategyTest {

    private TradingPairDto btcUsdt;
    private MarketQuote quote;

    @BeforeEach
    void setUp() {
        btcUsdt = new TradingPairDto();
        btcUsdt.setId(1L);
        btcUsdt.setPairName("BTCUSDT");
        btcUsdt.setBaseSymbolId(1L);
        btcUsdt.setBaseSymbol("BTC");
        btcUsdt.setQuoteSymbolId(3L);
        btcUsdt.setQuoteSymbol("USDT");
        btcUsdt.setActive(true);
        // Prices with many decimals so quantity x price needs rounding at 8 dp.
        quote = new MarketQuote(1L, new BigDecimal("45000.123456789"), "HUOBI",
                new BigDecimal("45001.987654321"), "BINANCE", LocalDateTime.now());
    }

    @Test
    void buy_ShouldUseAskAndRoundCostUp() {
        TradeSettlement settlement = new BuyTradeStrategy().settle(btcUsdt, quote, new BigDecimal("0.00000003"));

        // exact cost = 0.00135005962962963 -> rounded up
        assertThat(settlement.executionPrice()).isEqualByComparingTo("45001.987654321");
        assertThat(settlement.priceSource()).isEqualTo("BINANCE");
        assertThat(settlement.totalAmount()).isEqualByComparingTo("0.00135006");
        assertThat(settlement.debit().symbol()).isEqualTo("USDT");
        assertThat(settlement.debit().amount()).isEqualByComparingTo("0.00135006");
        assertThat(settlement.credit().symbol()).isEqualTo("BTC");
        assertThat(settlement.credit().amount()).isEqualByComparingTo("0.00000003");
    }

    @Test
    void sell_ShouldUseBidAndRoundProceedsDown() {
        TradeSettlement settlement = new SellTradeStrategy().settle(btcUsdt, quote, new BigDecimal("0.00000003"));

        // exact proceeds = 0.00135000370370367 -> rounded down
        assertThat(settlement.executionPrice()).isEqualByComparingTo("45000.123456789");
        assertThat(settlement.priceSource()).isEqualTo("HUOBI");
        assertThat(settlement.totalAmount()).isEqualByComparingTo("0.00135000");
        assertThat(settlement.debit().symbol()).isEqualTo("BTC");
        assertThat(settlement.debit().amount()).isEqualByComparingTo("0.00000003");
        assertThat(settlement.credit().symbol()).isEqualTo("USDT");
        assertThat(settlement.credit().amount()).isEqualByComparingTo("0.00135000");
    }

    @Test
    void resolver_ShouldReturnStrategyForEachType() {
        TradeExecutionStrategyResolver resolver =
                new TradeExecutionStrategyResolver(List.of(new BuyTradeStrategy(), new SellTradeStrategy()));

        assertThat(resolver.resolve(TradeType.BUY)).isInstanceOf(BuyTradeStrategy.class);
        assertThat(resolver.resolve(TradeType.SELL)).isInstanceOf(SellTradeStrategy.class);
    }

    @Test
    void resolver_WhenStrategyMissing_ShouldThrow() {
        TradeExecutionStrategyResolver resolver = new TradeExecutionStrategyResolver(List.of(new BuyTradeStrategy()));

        assertThatThrownBy(() -> resolver.resolve(TradeType.SELL)).isInstanceOf(UnsupportedTradeTypeException.class);
        assertThatThrownBy(() -> resolver.resolve(null)).isInstanceOf(UnsupportedTradeTypeException.class);
    }

    @Test
    void resolver_WhenDuplicateStrategies_ShouldFailFast() {
        assertThatThrownBy(() -> new TradeExecutionStrategyResolver(List.of(new BuyTradeStrategy(), new BuyTradeStrategy())))
                .isInstanceOf(IllegalStateException.class);
    }
}
