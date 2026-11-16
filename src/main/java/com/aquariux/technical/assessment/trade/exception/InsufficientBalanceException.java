package com.aquariux.technical.assessment.trade.exception;

import org.springframework.http.HttpStatus;
import java.math.BigDecimal;

public class InsufficientBalanceException extends TradingException {

    public InsufficientBalanceException(String symbol, BigDecimal required) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "INSUFFICIENT_BALANCE", "Insufficient " + symbol + " balance: " + required.toPlainString() + " required");
    }
}
