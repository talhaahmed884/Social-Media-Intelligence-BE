package com.media.intelligence.common.dto.api_response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.media.intelligence.common.dto.error_response.ErrorResponse;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * API Success Response wrapper using Builder Pattern.
 * Provides a consistent response structure for successful API responses.
 * <p>
 * For error responses, use {@link ErrorResponse} instead.
 *
 * @param <T> the type of data payload
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private final boolean success;
    private final String message;
    private final T data;
    private final LocalDateTime timestamp;

    protected ApiResponse(ApiResponseBuilder<T> builder) {
        this.success = true; // Always true for success responses
        this.message = builder.message;
        this.data = builder.data;
        this.timestamp = builder.timestamp;
    }

    public static <T> ApiResponseBuilder<T> builder() {
        return new ApiResponseBuilder<>();
    }

    /**
     * Convenience method for successful response with data
     */
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .message("Request processed successfully")
                .data(data)
                .build();
    }

    /**
     * Convenience method for successful response with custom message
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .message(message)
                .data(data)
                .build();
    }
}
