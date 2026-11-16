package com.aquariux.technical.assessment.trade.dto.internal;

import java.math.BigDecimal;

/**
 * The wallet movements and pricing produced by a trade strategy.
 *
 * @param executionPrice price per unit of base asset, in the quote asset
 * @param priceSource    exchange the execution price came from
 * @param totalAmount    quantity x price, in the quote asset
 * @param debit          wallet reduced by the trade
 * @param credit         wallet increased by the trade
 */
public record TradeSettlement(
        BigDecimal executionPrice,
        String priceSource,
        BigDecimal totalAmount,
        WalletMovement debit,
        WalletMovement credit) {

    /**
     * @param symbolId symbol whose wallet moves
     * @param symbol   symbol code (BTC, ETH, USDT) for messages
     * @param amount   positive amount moved
     */
    public record WalletMovement(Long symbolId, String symbol, BigDecimal amount) {
    }
}
