package com.aquariux.technical.assessment.trade.controller;

import com.aquariux.technical.assessment.trade.config.SecurityConfig;
import com.aquariux.technical.assessment.trade.config.TradingConfig;
import com.aquariux.technical.assessment.trade.dto.request.TradeRequest;
import com.aquariux.technical.assessment.trade.dto.response.TradeResponse;
import com.aquariux.technical.assessment.trade.enums.TradeType;
import com.aquariux.technical.assessment.trade.exception.InsufficientBalanceException;
import com.aquariux.technical.assessment.trade.exception.UserNotFoundException;
import com.aquariux.technical.assessment.trade.service.TradeServiceInterface;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TradeController.class)
@Import({SecurityConfig.class, TradingConfig.class})
class TradeControllerTest {

    private static final String URL = "/api/trades/execute";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TradeServiceInterface tradeService;

    @Test
    void executeTrade_WhenValid_ShouldReturn201WithTrade() throws Exception {
        TradeResponse response = new TradeResponse();
        response.setTradeId(42L);
        response.setUserId(1L);
        response.setPairName("ETHUSDT");
        response.setTradeType(TradeType.BUY);
        response.setQuantity(new BigDecimal("0.5"));
        response.setPrice(new BigDecimal("3001"));
        response.setTotalAmount(new BigDecimal("1500.5"));
        response.setPriceSource("BINANCE");
        response.setTradeTime(LocalDateTime.of(2025, 1, 20, 10, 0));
        when(tradeService.executeTrade(any(TradeRequest.class))).thenReturn(response);

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":1,"tradeType":"BUY","pairName":"ETHUSDT","quantity":0.5}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tradeId").value(42))
                .andExpect(jsonPath("$.totalAmount").value(1500.5))
                .andExpect(jsonPath("$.priceSource").value("BINANCE"));
    }

    @Test
    void executeTrade_WhenFieldsMissing_ShouldReturn400WithFieldErrors() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":1,"tradeType":"BUY"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.length()").value(2));
        verifyNoInteractions(tradeService);
    }

    @Test
    void executeTrade_WhenQuantityInvalid_ShouldReturn400() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":1,"tradeType":"BUY","pairName":"ETHUSDT","quantity":0.000000001}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("quantity"));

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":1,"tradeType":"BUY","pairName":"ETHUSDT","quantity":-1}
                                """))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(tradeService);
    }

    @Test
    void executeTrade_WhenTradeTypeUnknown_ShouldReturn400() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":1,"tradeType":"HOLD","pairName":"ETHUSDT","quantity":1}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void executeTrade_WhenUserMissing_ShouldReturn404() throws Exception {
        when(tradeService.executeTrade(any(TradeRequest.class))).thenThrow(new UserNotFoundException(999L));

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":999,"tradeType":"BUY","pairName":"ETHUSDT","quantity":1}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
    }

    @Test
    void executeTrade_WhenInsufficientBalance_ShouldReturn422() throws Exception {
        when(tradeService.executeTrade(any(TradeRequest.class)))
                .thenThrow(new InsufficientBalanceException("USDT", new BigDecimal("300100")));

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":1,"tradeType":"BUY","pairName":"ETHUSDT","quantity":100}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_BALANCE"));
    }

    @Test
    void executeTrade_WhenUnexpectedError_ShouldReturn500WithoutDetails() throws Exception {
        when(tradeService.executeTrade(any(TradeRequest.class))).thenThrow(new IllegalStateException("db secret"));

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":1,"tradeType":"BUY","pairName":"ETHUSDT","quantity":1}
                                """))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred, please try again later"));
    }
}
