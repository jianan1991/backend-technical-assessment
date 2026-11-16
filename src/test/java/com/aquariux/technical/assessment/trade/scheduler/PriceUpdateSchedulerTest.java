package com.aquariux.technical.assessment.trade.scheduler;

import com.aquariux.technical.assessment.trade.entity.CryptoPrice;
import com.aquariux.technical.assessment.trade.mapper.CryptoPairMapper;
import com.aquariux.technical.assessment.trade.mapper.CryptoPriceMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PriceUpdateSchedulerTest {

    @Mock
    private RestTemplate restTemplate;
    @Mock
    private CryptoPairMapper cryptoPairMapper;
    @Mock
    private CryptoPriceMapper cryptoPriceMapper;

    @InjectMocks
    private PriceUpdateScheduler scheduler;

    @BeforeEach
    void setUp() {
        when(cryptoPairMapper.findIdByPairName("BTCUSDT")).thenReturn(1L);
        when(cryptoPairMapper.findIdByPairName("ETHUSDT")).thenReturn(2L);
    }

    private Map<String, CryptoPrice> storedPricesByPairId() {
        ArgumentCaptor<CryptoPrice> captor = ArgumentCaptor.forClass(CryptoPrice.class);
        verify(cryptoPriceMapper, times(2)).insertPrice(captor.capture());
        return Map.of(
                "BTCUSDT", captor.getAllValues().stream().filter(p -> p.getCryptoPairId() == 1L).findFirst().orElseThrow(),
                "ETHUSDT", captor.getAllValues().stream().filter(p -> p.getCryptoPairId() == 2L).findFirst().orElseThrow());
    }

    @Test
    void updatePrices_ShouldStoreHighestBidAndLowestAskUnderTheCorrectPair() {
        when(restTemplate.getForObject(contains("binance"), eq(List.class))).thenReturn(List.of(
                Map.of("symbol", "BTCUSDT", "bidPrice", "45000.00", "askPrice", "45010.00"),
                Map.of("symbol", "ETHUSDT", "bidPrice", "3000.00", "askPrice", "3002.00"),
                Map.of("symbol", "XRPUSDT", "bidPrice", "1.00", "askPrice", "1.01")));
        when(restTemplate.getForObject(contains("huobi"), eq(Map.class))).thenReturn(Map.of("data", List.of(
                Map.of("symbol", "btcusdt", "bid", 45005.00, "ask", 45020.00),
                Map.of("symbol", "ethusdt", "bid", 2999.00, "ask", 3001.00))));

        scheduler.updatePrices();

        Map<String, CryptoPrice> stored = storedPricesByPairId();

        CryptoPrice btc = stored.get("BTCUSDT");
        assertThat(btc.getBidPrice()).isEqualByComparingTo("45005.00");
        assertThat(btc.getBidSource()).isEqualTo("HUOBI");
        assertThat(btc.getAskPrice()).isEqualByComparingTo("45010.00");
        assertThat(btc.getAskSource()).isEqualTo("BINANCE");

        CryptoPrice eth = stored.get("ETHUSDT");
        assertThat(eth.getBidPrice()).isEqualByComparingTo("3000.00");
        assertThat(eth.getBidSource()).isEqualTo("BINANCE");
        assertThat(eth.getAskPrice()).isEqualByComparingTo("3001.00");
        assertThat(eth.getAskSource()).isEqualTo("HUOBI");
    }

    @Test
    void updatePrices_WhenOneExchangeFails_ShouldStillStoreTheOther() {
        when(restTemplate.getForObject(contains("binance"), eq(List.class))).thenReturn(List.of(
                Map.of("symbol", "BTCUSDT", "bidPrice", "45000.00", "askPrice", "45010.00"),
                Map.of("symbol", "ETHUSDT", "bidPrice", "3000.00", "askPrice", "3002.00")));
        when(restTemplate.getForObject(contains("huobi"), eq(Map.class))).thenThrow(new RestClientException("down"));

        scheduler.updatePrices();

        Map<String, CryptoPrice> stored = storedPricesByPairId();
        assertThat(stored.get("BTCUSDT").getBidPrice()).isEqualByComparingTo("45000.00");
        assertThat(stored.get("BTCUSDT").getAskPrice()).isEqualByComparingTo("45010.00");
        assertThat(stored.get("ETHUSDT").getAskSource()).isEqualTo("BINANCE");
    }
}
