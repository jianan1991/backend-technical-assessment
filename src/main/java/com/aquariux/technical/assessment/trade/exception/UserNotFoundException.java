package com.aquariux.technical.assessment.trade.exception;

import org.springframework.http.HttpStatus;

public class UserNotFoundException extends TradingException {

    public UserNotFoundException(Long userId) {
        super(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found: " + userId);
    }
}
