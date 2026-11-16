package com.aquariux.technical.assessment.trade.service.impl;

import com.aquariux.technical.assessment.trade.dto.internal.TradeSettlement.WalletMovement;
import com.aquariux.technical.assessment.trade.exception.InsufficientBalanceException;
import com.aquariux.technical.assessment.trade.mapper.UserWalletMapper;
import com.aquariux.technical.assessment.trade.service.WalletLedgerServiceInterface;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Applies balance changes with single atomic SQL statements so concurrent trades for the
 * same user cannot overdraw a wallet or lose an update.
 * Must run inside the caller's transaction so a failed trade rolls every movement back.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class WalletLedgerServiceImpl implements WalletLedgerServiceInterface {

    private final UserWalletMapper userWalletMapper;

    @Override
    public void debit(Long userId, WalletMovement movement, LocalDateTime at) {
        int updated = userWalletMapper.debit(userId, movement.symbolId(), movement.amount(), at);
        if (updated == 0) {
            throw new InsufficientBalanceException(movement.symbol(), movement.amount());
        }
    }

    @Override
    public void credit(Long userId, WalletMovement movement, LocalDateTime at) {
        if (userWalletMapper.credit(userId, movement.symbolId(), movement.amount(), at) > 0) {
            return;
        }
        // First acquisition of this symbol: the wallet record is created now (business rule).
        try {
            userWalletMapper.insert(userId, movement.symbolId(), movement.amount(), at);
            log.info("Created {} wallet for user {}", movement.symbol(), userId);
        } catch (DuplicateKeyException race) {
            // Another trade created the wallet between our UPDATE and INSERT; add to it instead.
            if (userWalletMapper.credit(userId, movement.symbolId(), movement.amount(), at) == 0) {
                throw new IllegalStateException(
                        "Unable to credit " + movement.symbol() + " wallet for user " + userId, race);
            }
        }
    }
}
