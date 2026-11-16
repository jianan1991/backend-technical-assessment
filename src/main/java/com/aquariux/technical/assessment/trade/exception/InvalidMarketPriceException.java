package com.aquariux.technical.assessment.trade.exception;

import org.springframework.http.HttpStatus;

public class InvalidMarketPriceException extends TradingException {

    public InvalidMarketPriceException(String message) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_MARKET_PRICE", message);
    }
}
