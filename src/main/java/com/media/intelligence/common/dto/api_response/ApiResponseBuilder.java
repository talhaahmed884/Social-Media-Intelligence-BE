package com.media.intelligence.common.dto.api_response;

import java.time.LocalDateTime;

public class ApiResponseBuilder<T> {
    protected String message;
    protected T data;
    protected LocalDateTime timestamp;

    protected ApiResponseBuilder() {
        this.timestamp = LocalDateTime.now();
    }

    public ApiResponseBuilder<T> message(String message) {
        this.message = message;
        return this;
    }

    public ApiResponseBuilder<T> data(T data) {
        this.data = data;
        return this;
    }

    public ApiResponseBuilder<T> timestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public ApiResponse<T> build() {
        return new ApiResponse<>(this);
    }
}
