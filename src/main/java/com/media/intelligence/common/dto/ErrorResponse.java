package com.media.intelligence.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * API Error Response wrapper using Builder Pattern.
 * Provides a consistent response structure for error responses.
 * <p>
 * For success responses, use {@link ApiResponse} instead.
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private final boolean success;
    private final String errorCode;
    private final String message;
    private final List<String> errors;
    private final LocalDateTime timestamp;

    private ErrorResponse(Builder builder) {
        this.success = false; // Always false for error responses
        this.errorCode = builder.errorCode;
        this.message = builder.message;
        this.errors = builder.errors;
        this.timestamp = builder.timestamp;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Convenience method for error response with single message
     */
    public static ErrorResponse error(String message) {
        return ErrorResponse.builder()
                .message(message)
                .build();
    }

    /**
     * Convenience method for error response with error code
     */
    public static ErrorResponse error(String errorCode, String message) {
        return ErrorResponse.builder()
                .errorCode(errorCode)
                .message(message)
                .build();
    }

    /**
     * Convenience method for error response with multiple errors
     */
    public static ErrorResponse error(String message, List<String> errors) {
        return ErrorResponse.builder()
                .message(message)
                .errors(errors)
                .build();
    }

    /**
     * Convenience method for error response with error code and multiple errors
     */
    public static ErrorResponse error(String errorCode, String message, List<String> errors) {
        return ErrorResponse.builder()
                .errorCode(errorCode)
                .message(message)
                .errors(errors)
                .build();
    }

    public static class Builder {
        private String errorCode;
        private String message;
        private List<String> errors;
        private LocalDateTime timestamp;

        private Builder() {
            this.timestamp = LocalDateTime.now();
        }

        public Builder errorCode(String errorCode) {
            this.errorCode = errorCode;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder errors(List<String> errors) {
            this.errors = errors;
            return this;
        }

        public Builder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public ErrorResponse build() {
            return new ErrorResponse(this);
        }
    }
}
