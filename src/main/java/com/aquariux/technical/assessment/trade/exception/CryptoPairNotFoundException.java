package com.aquariux.technical.assessment.trade.exception;

import org.springframework.http.HttpStatus;

public class CryptoPairNotFoundException extends TradingException {

    public CryptoPairNotFoundException(String pairName) {
        super(HttpStatus.NOT_FOUND, "PAIR_NOT_FOUND", "Trading pair not found or inactive: " + pairName);
    }
}
