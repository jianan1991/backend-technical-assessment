package com.aquariux.technical.assessment.trade.exception;

import org.springframework.http.HttpStatus;

public class PriceUnavailableException extends TradingException {

    public PriceUnavailableException(String message) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "PRICE_UNAVAILABLE", message);
    }
}
