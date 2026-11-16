package com.aquariux.technical.assessment.trade.service;

import com.aquariux.technical.assessment.trade.dto.internal.TradeSettlement.WalletMovement;

import java.time.LocalDateTime;

/**
 * Write-side wallet operations used by the trade engine
 * (kept separate from the read-only {@link WalletServiceInterface}).
 */
public interface WalletLedgerServiceInterface {

    /**
     * Removes funds from the user's wallet.
     *
     * @throws com.aquariux.technical.assessment.trade.exception.InsufficientBalanceException if the wallet is missing or holds too little
     */
    void debit(Long userId, WalletMovement movement, LocalDateTime at);

    /**
     * Adds funds to the user's wallet, creating the wallet on first acquisition of a symbol.
     */
    void credit(Long userId, WalletMovement movement, LocalDateTime at);
}
