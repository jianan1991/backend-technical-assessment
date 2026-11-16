package com.aquariux.technical.assessment.trade.exception;

import org.springframework.http.HttpStatus;

public class InvalidTradeAmountException extends TradingException {

    public InvalidTradeAmountException(String message) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_TRADE_AMOUNT", message);
    }
}
