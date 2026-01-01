package com.media.intelligence.common.dto.error_response;

import java.time.LocalDateTime;
import java.util.List;

public class ErrorResponseBuilder {
    protected String errorCode;
    protected String message;
    protected List<String> errors;
    protected LocalDateTime timestamp;

    protected ErrorResponseBuilder() {
        this.timestamp = LocalDateTime.now();
    }

    public ErrorResponseBuilder errorCode(String errorCode) {
        this.errorCode = errorCode;
        return this;
    }

    public ErrorResponseBuilder message(String message) {
        this.message = message;
        return this;
    }

    public ErrorResponseBuilder errors(List<String> errors) {
        this.errors = errors;
        return this;
    }

    public ErrorResponseBuilder timestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public ErrorResponse build() {
        return new ErrorResponse(this);
    }
}
