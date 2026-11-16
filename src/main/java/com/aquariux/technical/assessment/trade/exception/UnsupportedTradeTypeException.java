package com.aquariux.technical.assessment.trade.exception;

import org.springframework.http.HttpStatus;

public class UnsupportedTradeTypeException extends TradingException {

    public UnsupportedTradeTypeException(Object tradeType) {
        super(HttpStatus.BAD_REQUEST, "UNSUPPORTED_TRADE_TYPE", "Unsupported trade type: " + tradeType);
    }
}
