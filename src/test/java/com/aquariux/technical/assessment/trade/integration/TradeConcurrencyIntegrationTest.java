package com.aquariux.technical.assessment.trade.integration;

import com.aquariux.technical.assessment.trade.dto.request.TradeRequest;
import com.aquariux.technical.assessment.trade.entity.CryptoPrice;
import com.aquariux.technical.assessment.trade.enums.TradeType;
import com.aquariux.technical.assessment.trade.exception.InsufficientBalanceException;
import com.aquariux.technical.assessment.trade.mapper.CryptoPriceMapper;
import com.aquariux.technical.assessment.trade.mapper.UserWalletMapper;
import com.aquariux.technical.assessment.trade.service.TradeServiceInterface;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Fires many simultaneous SELL orders that together exceed the user's balance and checks that
 * the wallet is never overdrawn. Uses its own in-memory database because trades really commit here.
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:concurrency-test")
class TradeConcurrencyIntegrationTest {

    private static final long USER_ID = 8L;     // seed: 1.0 BTC, 45,000 USDT
    private static final long BTCUSDT = 1L;
    private static final long BTC = 1L;
    private static final long USDT = 3L;

    @Autowired
    private TradeServiceInterface tradeService;
    @Autowired
    private CryptoPriceMapper cryptoPriceMapper;
    @Autowired
    private UserWalletMapper userWalletMapper;

    @Test
    void concurrentSells_ShouldNeverOverdrawWallet() throws Exception {
        CryptoPrice price = new CryptoPrice();
        price.setCryptoPairId(BTCUSDT);
        price.setBidPrice(new BigDecimal("45000"));
        price.setAskPrice(new BigDecimal("45010"));
        price.setBidSource("BINANCE");
        price.setAskSource("HUOBI");
        cryptoPriceMapper.insertPrice(price);

        int attempts = 20; // 20 x 0.1 BTC = 2 BTC requested, only 1 BTC available
        ExecutorService pool = Executors.newFixedThreadPool(attempts);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();
        for (int i = 0; i < attempts; i++) {
            Callable<Boolean> sell = () -> {
                start.await();
                TradeRequest request = new TradeRequest();
                request.setUserId(USER_ID);
                request.setTradeType(TradeType.SELL);
                request.setPairName("BTCUSDT");
                request.setQuantity(new BigDecimal("0.1"));
                try {
                    tradeService.executeTrade(request);
                    return true;
                } catch (InsufficientBalanceException expected) {
                    return false;
                }
            };
            results.add(pool.submit(sell));
        }
        start.countDown();

        int succeeded = 0;
        for (Future<Boolean> result : results) {
            if (result.get()) {
                succeeded++;
            }
        }
        pool.shutdown();

        assertThat(succeeded).isEqualTo(10);
        assertThat(userWalletMapper.findBalance(USER_ID, BTC)).isEqualByComparingTo("0");
        assertThat(userWalletMapper.findBalance(USER_ID, USDT)).isEqualByComparingTo("90000"); // 45,000 + 10 x 4,500
    }
}
