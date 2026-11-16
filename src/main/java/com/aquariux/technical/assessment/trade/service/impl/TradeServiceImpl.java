package com.aquariux.technical.assessment.trade.service.impl;

import com.aquariux.technical.assessment.trade.dto.internal.MarketQuote;
import com.aquariux.technical.assessment.trade.dto.internal.TradeSettlement;
import com.aquariux.technical.assessment.trade.dto.internal.TradingPairDto;
import com.aquariux.technical.assessment.trade.dto.request.TradeRequest;
import com.aquariux.technical.assessment.trade.dto.response.TradeResponse;
import com.aquariux.technical.assessment.trade.entity.Trade;
import com.aquariux.technical.assessment.trade.enums.TradeType;
import com.aquariux.technical.assessment.trade.exception.CryptoPairNotFoundException;
import com.aquariux.technical.assessment.trade.exception.InvalidTradeAmountException;
import com.aquariux.technical.assessment.trade.exception.UserNotFoundException;
import com.aquariux.technical.assessment.trade.mapper.CryptoPairMapper;
import com.aquariux.technical.assessment.trade.mapper.TradeMapper;
import com.aquariux.technical.assessment.trade.mapper.UserMapper;
import com.aquariux.technical.assessment.trade.service.MarketPriceProviderInterface;
import com.aquariux.technical.assessment.trade.service.TradeServiceInterface;
import com.aquariux.technical.assessment.trade.service.WalletLedgerServiceInterface;
import com.aquariux.technical.assessment.trade.service.strategy.TradeAmounts;
import com.aquariux.technical.assessment.trade.service.strategy.TradeExecutionStrategyResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;

/**
 * Orchestrates a market order: validate -> price -> settle -> move funds -> record.
 * Each step is delegated to a collaborator; everything runs in one transaction, so a trade
 * either fully happens (both wallets updated and the trade recorded) or not at all.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TradeServiceImpl implements TradeServiceInterface {

    /** DECIMAL(20,8) leaves 12 integer digits. */
    private static final int MAX_INTEGER_DIGITS = 12;

    private final TradeMapper tradeMapper;
    private final UserMapper userMapper;
    private final CryptoPairMapper cryptoPairMapper;
    private final MarketPriceProviderInterface marketPriceProvider;
    private final TradeExecutionStrategyResolver strategyResolver;
    private final WalletLedgerServiceInterface walletLedgerService;
    private final Clock clock;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TradeResponse executeTrade(TradeRequest tradeRequest) {
        Long userId = tradeRequest.getUserId();
        ensureUserExists(userId);

        TradingPairDto pair = findActivePair(tradeRequest.getPairName());
        BigDecimal quantity = tradeRequest.getQuantity().setScale(TradeAmounts.SCALE, RoundingMode.UNNECESSARY);
        MarketQuote quote = marketPriceProvider.getTradableQuote(pair);

        TradeSettlement settlement = strategyResolver.resolve(tradeRequest.getTradeType())
                .settle(pair, quote, quantity);
        validateTotal(settlement.totalAmount());

        LocalDateTime executedAt = LocalDateTime.now(clock);
        walletLedgerService.debit(userId, settlement.debit(), executedAt);
        walletLedgerService.credit(userId, settlement.credit(), executedAt);

        Trade trade = buildTrade(tradeRequest, pair, quantity, settlement, executedAt);
        tradeMapper.insert(trade);

        log.info("Trade {} executed: user={} {} {} {} @ {} ({}) total={} {}",
                trade.getId(), userId, trade.getTradeType(), quantity.toPlainString(), pair.getPairName(),
                settlement.executionPrice().toPlainString(), settlement.priceSource(),
                settlement.totalAmount().toPlainString(), pair.getQuoteSymbol());

        return toResponse(trade, pair, settlement);
    }

    private void ensureUserExists(Long userId) {
        if (!userMapper.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }
    }

    private TradingPairDto findActivePair(String requestedPairName) {
        String pairName = requestedPairName.trim().toUpperCase(Locale.ROOT);
        TradingPairDto pair = cryptoPairMapper.findTradingPairByName(pairName);
        if (pair == null || !Boolean.TRUE.equals(pair.getActive())) {
            throw new CryptoPairNotFoundException(pairName);
        }
        return pair;
    }

    private void validateTotal(BigDecimal totalAmount) {
        if (totalAmount.signum() <= 0) {
            throw new InvalidTradeAmountException("Trade value is too small, please increase the quantity");
        }
        if (totalAmount.precision() - totalAmount.scale() > MAX_INTEGER_DIGITS) {
            throw new InvalidTradeAmountException("Trade value is too large, please reduce the quantity");
        }
    }

    private Trade buildTrade(TradeRequest request, TradingPairDto pair, BigDecimal quantity,
                             TradeSettlement settlement, LocalDateTime executedAt) {
        Trade trade = new Trade();
        trade.setUserId(request.getUserId());
        trade.setCryptoPairId(pair.getId());
        trade.setTradeType(request.getTradeType().name());
        trade.setQuantity(quantity);
        trade.setPrice(settlement.executionPrice());
        trade.setTotalAmount(settlement.totalAmount());
        trade.setTradeTime(executedAt);
        return trade;
    }

    private TradeResponse toResponse(Trade trade, TradingPairDto pair, TradeSettlement settlement) {
        TradeResponse response = new TradeResponse();
        response.setTradeId(trade.getId());
        response.setUserId(trade.getUserId());
        response.setPairName(pair.getPairName());
        response.setTradeType(TradeType.valueOf(trade.getTradeType()));
        response.setQuantity(trade.getQuantity());
        response.setPrice(trade.getPrice());
        response.setTotalAmount(trade.getTotalAmount());
        response.setPriceSource(settlement.priceSource());
        response.setTradeTime(trade.getTradeTime());
        return response;
    }
}
