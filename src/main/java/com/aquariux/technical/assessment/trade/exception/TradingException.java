package com.aquariux.technical.assessment.trade.exception;

import org.springframework.http.HttpStatus;

/**
 * Base type for expected, client-facing trading errors. Each subclass maps to one HTTP status
 * and a stable error code, so the exception handler stays closed for modification.
 */
public abstract class TradingException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    protected TradingException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }
}
