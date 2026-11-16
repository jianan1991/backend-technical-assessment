package com.aquariux.technical.assessment.trade.integration;

import com.aquariux.technical.assessment.trade.entity.CryptoPrice;
import com.aquariux.technical.assessment.trade.entity.Trade;
import com.aquariux.technical.assessment.trade.mapper.CryptoPriceMapper;
import com.aquariux.technical.assessment.trade.mapper.TradeMapper;
import com.aquariux.technical.assessment.trade.mapper.UserWalletMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end through HTTP, MyBatis and the Flyway-seeded H2 database.
 * Each test rolls back, so seeded data stays intact between tests.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TradeExecutionIntegrationTest {

    private static final long ETHUSDT = 2L;
    private static final long ETH = 2L;
    private static final long USDT = 3L;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private CryptoPriceMapper cryptoPriceMapper;
    @Autowired
    private UserWalletMapper userWalletMapper;
    @Autowired
    private TradeMapper tradeMapper;

    private void givenValidEthQuote() {
        // Seed quotes are crossed (pre-existing data bug), so store a sane, newer quote.
        CryptoPrice price = new CryptoPrice();
        price.setCryptoPairId(ETHUSDT);
        price.setBidPrice(new BigDecimal("3000.00"));
        price.setAskPrice(new BigDecimal("3001.00"));
        price.setBidSource("HUOBI");
        price.setAskSource("BINANCE");
        cryptoPriceMapper.insertPrice(price);
    }

    private JsonNode trade(long userId, String type, String pair, String quantity, int expectedStatus) throws Exception {
        String body = mockMvc.perform(post("/api/trades/execute").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":%d,"tradeType":"%s","pairName":"%s","quantity":%s}
                                """.formatted(userId, type, pair, quantity)))
                .andExpect(status().is(expectedStatus))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body);
    }

    @Test
    void buyThenSell_ShouldMoveFundsCreateWalletAndRecordTrades() throws Exception {
        givenValidEthQuote();
        // user1 seed: 10,000 USDT and no ETH wallet
        assertThat(userWalletMapper.findBalance(1L, ETH)).isNull();

        JsonNode buy = trade(1L, "BUY", "ETHUSDT", "0.5", 201);
        assertThat(buy.get("price").decimalValue()).isEqualByComparingTo("3001.00");
        assertThat(buy.get("totalAmount").decimalValue()).isEqualByComparingTo("1500.5");
        assertThat(userWalletMapper.findBalance(1L, USDT)).isEqualByComparingTo("8499.5");
        assertThat(userWalletMapper.findBalance(1L, ETH)).isEqualByComparingTo("0.5");

        JsonNode sell = trade(1L, "SELL", "ETHUSDT", "0.2", 201);
        assertThat(sell.get("price").decimalValue()).isEqualByComparingTo("3000.00");
        assertThat(userWalletMapper.findBalance(1L, USDT)).isEqualByComparingTo("9099.5");
        assertThat(userWalletMapper.findBalance(1L, ETH)).isEqualByComparingTo("0.3");

        Trade recorded = tradeMapper.findById(sell.get("tradeId").asLong());
        assertThat(recorded.getUserId()).isEqualTo(1L);
        assertThat(recorded.getTradeType()).isEqualTo("SELL");
        assertThat(recorded.getQuantity()).isEqualByComparingTo("0.2");
        assertThat(recorded.getTotalAmount()).isEqualByComparingTo("600");
        assertThat(recorded.getTradeTime()).isNotNull();
    }

    @Test
    void buy_WhenInsufficientBalance_ShouldReturn422AndChangeNothing() throws Exception {
        givenValidEthQuote();
        int tradesBefore = tradeMapper.findByUserId(1L).size();

        JsonNode error = trade(1L, "BUY", "ETHUSDT", "10", 422);

        assertThat(error.get("code").asText()).isEqualTo("INSUFFICIENT_BALANCE");
        assertThat(userWalletMapper.findBalance(1L, USDT)).isEqualByComparingTo("10000");
        assertThat(userWalletMapper.findBalance(1L, ETH)).isNull();
        assertThat(tradeMapper.findByUserId(1L)).hasSize(tradesBefore);
    }

    @Test
    void sell_WhenUserHasNoWalletForAsset_ShouldReturn422() throws Exception {
        givenValidEthQuote();

        JsonNode error = trade(1L, "SELL", "ETHUSDT", "0.1", 422);

        assertThat(error.get("code").asText()).isEqualTo("INSUFFICIENT_BALANCE");
    }

    @Test
    void trade_WhenLatestQuoteIsCrossed_ShouldUseTheWorsePriceForEachSide() throws Exception {
        // Exchanges disagree: HUOBI's bid (3005) is above BINANCE's ask (3000)
        CryptoPrice crossed = new CryptoPrice();
        crossed.setCryptoPairId(ETHUSDT);
        crossed.setBidPrice(new BigDecimal("3005.00"));
        crossed.setAskPrice(new BigDecimal("3000.00"));
        crossed.setBidSource("HUOBI");
        crossed.setAskSource("BINANCE");
        cryptoPriceMapper.insertPrice(crossed);

        JsonNode buy = trade(9L, "BUY", "ETHUSDT", "1", 201);
        assertThat(buy.get("price").decimalValue()).isEqualByComparingTo("3005.00");
        assertThat(buy.get("priceSource").asText()).isEqualTo("HUOBI");

        JsonNode sell = trade(9L, "SELL", "ETHUSDT", "1", 201);
        assertThat(sell.get("price").decimalValue()).isEqualByComparingTo("3000.00");
        assertThat(sell.get("priceSource").asText()).isEqualTo("BINANCE");

        // Buying and selling straight back loses the gap instead of making a free profit
        assertThat(userWalletMapper.findBalance(9L, USDT)).isEqualByComparingTo("99995");
        assertThat(userWalletMapper.findBalance(9L, ETH)).isEqualByComparingTo("15");
    }

    @Test
    void trade_WhenUserOrPairUnknown_ShouldReturn404() throws Exception {
        assertThat(trade(999L, "BUY", "ETHUSDT", "1", 404).get("code").asText()).isEqualTo("USER_NOT_FOUND");
        assertThat(trade(1L, "BUY", "DOGEUSDT", "1", 404).get("code").asText()).isEqualTo("PAIR_NOT_FOUND");
    }
}
