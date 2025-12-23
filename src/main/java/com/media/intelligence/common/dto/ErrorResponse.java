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

    protected ErrorResponse(ErrorResponseBuilder builder) {
        this.success = false; // Always false for error responses
        this.errorCode = builder.errorCode;
        this.message = builder.message;
        this.errors = builder.errors;
        this.timestamp = builder.timestamp;
    }

    public static ErrorResponseBuilder builder() {
        return new ErrorResponseBuilder();
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
}
