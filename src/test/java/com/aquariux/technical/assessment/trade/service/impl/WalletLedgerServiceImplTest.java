package com.aquariux.technical.assessment.trade.service.impl;

import com.aquariux.technical.assessment.trade.dto.internal.TradeSettlement.WalletMovement;
import com.aquariux.technical.assessment.trade.exception.InsufficientBalanceException;
import com.aquariux.technical.assessment.trade.mapper.UserWalletMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalletLedgerServiceImplTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2025, 1, 20, 10, 0);
    private static final BigDecimal AMOUNT = new BigDecimal("100.00000000");
    private static final WalletMovement USDT = new WalletMovement(3L, "USDT", AMOUNT);

    @Mock
    private UserWalletMapper userWalletMapper;

    @InjectMocks
    private WalletLedgerServiceImpl ledger;

    @Test
    void debit_WhenBalanceSufficient_ShouldSucceed() {
        when(userWalletMapper.debit(1L, 3L, AMOUNT, NOW)).thenReturn(1);

        ledger.debit(1L, USDT, NOW);

        verify(userWalletMapper).debit(1L, 3L, AMOUNT, NOW);
    }

    @Test
    void debit_WhenNoRowUpdated_ShouldThrowInsufficientBalance() {
        when(userWalletMapper.debit(1L, 3L, AMOUNT, NOW)).thenReturn(0);

        assertThatThrownBy(() -> ledger.debit(1L, USDT, NOW))
                .isInstanceOf(InsufficientBalanceException.class)
                .hasMessageContaining("USDT");
    }

    @Test
    void credit_WhenWalletExists_ShouldNotInsert() {
        when(userWalletMapper.credit(1L, 3L, AMOUNT, NOW)).thenReturn(1);

        ledger.credit(1L, USDT, NOW);

        verify(userWalletMapper, never()).insert(any(), any(), any(), any());
    }

    @Test
    void credit_WhenWalletMissing_ShouldCreateIt() {
        when(userWalletMapper.credit(1L, 3L, AMOUNT, NOW)).thenReturn(0);

        ledger.credit(1L, USDT, NOW);

        verify(userWalletMapper).insert(1L, 3L, AMOUNT, NOW);
    }

    @Test
    void credit_WhenWalletCreatedConcurrently_ShouldRetryUpdate() {
        when(userWalletMapper.credit(1L, 3L, AMOUNT, NOW)).thenReturn(0, 1);
        when(userWalletMapper.insert(1L, 3L, AMOUNT, NOW)).thenThrow(new DuplicateKeyException("dup"));

        ledger.credit(1L, USDT, NOW);

        verify(userWalletMapper, times(2)).credit(1L, 3L, AMOUNT, NOW);
    }
}
