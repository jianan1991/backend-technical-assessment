package com.aquariux.technical.assessment.trade.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.List;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ErrorResponse {
    LocalDateTime timestamp;
    int status;
    /** Stable, machine-readable error code (e.g. INSUFFICIENT_BALANCE). */
    String code;
    String message;
    String path;
    List<FieldError> fieldErrors;

    @Value
    public static class FieldError {
        String field;
        String message;
    }
}
