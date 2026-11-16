package com.aquariux.technical.assessment.trade.service.impl;

import com.aquariux.technical.assessment.trade.dto.internal.MarketQuote;
import com.aquariux.technical.assessment.trade.dto.internal.TradeSettlement.WalletMovement;
import com.aquariux.technical.assessment.trade.dto.internal.TradingPairDto;
import com.aquariux.technical.assessment.trade.dto.request.TradeRequest;
import com.aquariux.technical.assessment.trade.dto.response.TradeResponse;
import com.aquariux.technical.assessment.trade.entity.Trade;
import com.aquariux.technical.assessment.trade.enums.TradeType;
import com.aquariux.technical.assessment.trade.exception.CryptoPairNotFoundException;
import com.aquariux.technical.assessment.trade.exception.InsufficientBalanceException;
import com.aquariux.technical.assessment.trade.exception.InvalidTradeAmountException;
import com.aquariux.technical.assessment.trade.exception.PriceUnavailableException;
import com.aquariux.technical.assessment.trade.exception.UserNotFoundException;
import com.aquariux.technical.assessment.trade.mapper.CryptoPairMapper;
import com.aquariux.technical.assessment.trade.mapper.TradeMapper;
import com.aquariux.technical.assessment.trade.mapper.UserMapper;
import com.aquariux.technical.assessment.trade.service.MarketPriceProviderInterface;
import com.aquariux.technical.assessment.trade.service.WalletLedgerServiceInterface;
import com.aquariux.technical.assessment.trade.service.strategy.BuyTradeStrategy;
import com.aquariux.technical.assessment.trade.service.strategy.SellTradeStrategy;
import com.aquariux.technical.assessment.trade.service.strategy.TradeExecutionStrategyResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TradeServiceImplTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2025-01-20T10:00:00Z"), ZoneOffset.UTC);
    private static final LocalDateTime NOW = LocalDateTime.now(CLOCK);

    @Mock
    private TradeMapper tradeMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private CryptoPairMapper cryptoPairMapper;
    @Mock
    private MarketPriceProviderInterface marketPriceProvider;
    @Mock
    private WalletLedgerServiceInterface walletLedgerService;

    private TradeServiceImpl tradeService;
    private TradingPairDto ethUsdt;
    private MarketQuote quote;

    @BeforeEach
    void setUp() {
        TradeExecutionStrategyResolver resolver =
                new TradeExecutionStrategyResolver(List.of(new BuyTradeStrategy(), new SellTradeStrategy()));
        tradeService = new TradeServiceImpl(tradeMapper, userMapper, cryptoPairMapper, marketPriceProvider,
                resolver, walletLedgerService, CLOCK);

        ethUsdt = new TradingPairDto();
        ethUsdt.setId(2L);
        ethUsdt.setPairName("ETHUSDT");
        ethUsdt.setBaseSymbolId(2L);
        ethUsdt.setBaseSymbol("ETH");
        ethUsdt.setQuoteSymbolId(3L);
        ethUsdt.setQuoteSymbol("USDT");
        ethUsdt.setActive(true);

        quote = new MarketQuote(2L, new BigDecimal("3000.00"), "HUOBI",
                new BigDecimal("3001.00"), "BINANCE", NOW.minusSeconds(5));
    }

    private TradeRequest request(TradeType type, String quantity) {
        TradeRequest request = new TradeRequest();
        request.setUserId(1L);
        request.setTradeType(type);
        request.setPairName("ethusdt ");
        request.setQuantity(new BigDecimal(quantity));
        return request;
    }

    private void givenValidUserPairAndQuote() {
        when(userMapper.existsById(1L)).thenReturn(true);
        when(cryptoPairMapper.findTradingPairByName("ETHUSDT")).thenReturn(ethUsdt);
        when(marketPriceProvider.getTradableQuote(ethUsdt)).thenReturn(quote);
    }

    @Test
    void executeTrade_Buy_ShouldDebitQuoteCreditBaseAndRecordTradeAtAsk() {
        givenValidUserPairAndQuote();
        doAnswer(invocation -> {
            invocation.<Trade>getArgument(0).setId(99L);
            return 1;
        }).when(tradeMapper).insert(any(Trade.class));

        TradeResponse response = tradeService.executeTrade(request(TradeType.BUY, "0.5"));

        verify(walletLedgerService).debit(1L, new WalletMovement(3L, "USDT", new BigDecimal("1500.50000000")), NOW);
        verify(walletLedgerService).credit(1L, new WalletMovement(2L, "ETH", new BigDecimal("0.50000000")), NOW);

        ArgumentCaptor<Trade> captor = ArgumentCaptor.forClass(Trade.class);
        verify(tradeMapper).insert(captor.capture());
        Trade saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(1L);
        assertThat(saved.getCryptoPairId()).isEqualTo(2L);
        assertThat(saved.getTradeType()).isEqualTo("BUY");
        assertThat(saved.getPrice()).isEqualByComparingTo("3001.00");
        assertThat(saved.getTotalAmount()).isEqualByComparingTo("1500.5");
        assertThat(saved.getTradeTime()).isEqualTo(NOW);

        assertThat(response.getTradeId()).isEqualTo(99L);
        assertThat(response.getPairName()).isEqualTo("ETHUSDT");
        assertThat(response.getTradeType()).isEqualTo(TradeType.BUY);
        assertThat(response.getQuantity()).isEqualByComparingTo("0.5");
        assertThat(response.getPrice()).isEqualByComparingTo("3001.00");
        assertThat(response.getTotalAmount()).isEqualByComparingTo("1500.5");
        assertThat(response.getPriceSource()).isEqualTo("BINANCE");
        assertThat(response.getTradeTime()).isEqualTo(NOW);
    }

    @Test
    void executeTrade_Sell_ShouldDebitBaseCreditQuoteAtBid() {
        givenValidUserPairAndQuote();

        TradeResponse response = tradeService.executeTrade(request(TradeType.SELL, "0.25"));

        verify(walletLedgerService).debit(1L, new WalletMovement(2L, "ETH", new BigDecimal("0.25000000")), NOW);
        verify(walletLedgerService).credit(1L, new WalletMovement(3L, "USDT", new BigDecimal("750.00000000")), NOW);
        assertThat(response.getPrice()).isEqualByComparingTo("3000.00");
        assertThat(response.getPriceSource()).isEqualTo("HUOBI");
        assertThat(response.getTradeType()).isEqualTo(TradeType.SELL);
    }

    @Test
    void executeTrade_WhenUserMissing_ShouldRejectWithoutTouchingWallets() {
        when(userMapper.existsById(1L)).thenReturn(false);

        assertThatThrownBy(() -> tradeService.executeTrade(request(TradeType.BUY, "1")))
                .isInstanceOf(UserNotFoundException.class);

        verifyNoInteractions(walletLedgerService, tradeMapper, marketPriceProvider);
    }

    @Test
    void executeTrade_WhenPairUnknown_ShouldReject() {
        when(userMapper.existsById(1L)).thenReturn(true);
        when(cryptoPairMapper.findTradingPairByName("ETHUSDT")).thenReturn(null);

        assertThatThrownBy(() -> tradeService.executeTrade(request(TradeType.BUY, "1")))
                .isInstanceOf(CryptoPairNotFoundException.class)
                .hasMessageContaining("ETHUSDT");
        verifyNoInteractions(walletLedgerService, tradeMapper);
    }

    @Test
    void executeTrade_WhenPairInactive_ShouldReject() {
        ethUsdt.setActive(false);
        when(userMapper.existsById(1L)).thenReturn(true);
        when(cryptoPairMapper.findTradingPairByName("ETHUSDT")).thenReturn(ethUsdt);

        assertThatThrownBy(() -> tradeService.executeTrade(request(TradeType.BUY, "1")))
                .isInstanceOf(CryptoPairNotFoundException.class);
        verifyNoInteractions(walletLedgerService, tradeMapper);
    }

    @Test
    void executeTrade_WhenPriceUnavailable_ShouldReject() {
        when(userMapper.existsById(1L)).thenReturn(true);
        when(cryptoPairMapper.findTradingPairByName("ETHUSDT")).thenReturn(ethUsdt);
        when(marketPriceProvider.getTradableQuote(ethUsdt)).thenThrow(new PriceUnavailableException("stale"));

        assertThatThrownBy(() -> tradeService.executeTrade(request(TradeType.BUY, "1")))
                .isInstanceOf(PriceUnavailableException.class);
        verifyNoInteractions(walletLedgerService, tradeMapper);
    }

    @Test
    void executeTrade_WhenInsufficientBalance_ShouldNotCreditOrRecordTrade() {
        givenValidUserPairAndQuote();
        doThrow(new InsufficientBalanceException("USDT", new BigDecimal("30010")))
                .when(walletLedgerService).debit(eq(1L), any(), any());

        assertThatThrownBy(() -> tradeService.executeTrade(request(TradeType.BUY, "10")))
                .isInstanceOf(InsufficientBalanceException.class);

        verify(walletLedgerService, never()).credit(any(), any(), any());
        verify(tradeMapper, never()).insert(any());
    }

    @Test
    void executeTrade_WhenSellProceedsRoundToZero_ShouldReject() {
        quote = new MarketQuote(2L, new BigDecimal("0.5"), "HUOBI", new BigDecimal("0.6"), "BINANCE", NOW);
        givenValidUserPairAndQuote();

        assertThatThrownBy(() -> tradeService.executeTrade(request(TradeType.SELL, "0.00000001")))
                .isInstanceOf(InvalidTradeAmountException.class);
        verifyNoInteractions(walletLedgerService, tradeMapper);
    }

    @Test
    void executeTrade_WhenTotalExceedsStorageRange_ShouldReject() {
        givenValidUserPairAndQuote();

        assertThatThrownBy(() -> tradeService.executeTrade(request(TradeType.BUY, "999999999999")))
                .isInstanceOf(InvalidTradeAmountException.class)
                .hasMessageContaining("too large");
        verifyNoInteractions(walletLedgerService, tradeMapper);
    }
}
