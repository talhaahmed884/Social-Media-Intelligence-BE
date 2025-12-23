package com.media.intelligence.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Interface defining the contract for error codes across the application.
 * Each domain can implement this interface with domain-specific error codes.
 * <p>
 * Benefits:
 * - Decouples error handling from specific exception types
 * - Allows domain-specific error codes (UserErrorCode, ConnectorErrorCode, etc.)
 * - Enables dynamic error code extraction in exception handlers
 * - Provides consistent error structure across all API responses
 * - Each error code defines its own HTTP status
 * <p>
 * Example implementations:
 * - CommonErrorCode (validation, system errors)
 * - UserErrorCode (user domain errors)
 * - ConnectorErrorCode (social media integration errors)
 */
public interface ErrorCode {

    /**
     * Get the unique error code identifier.
     * Should follow a consistent format: DOMAIN_ERROR_TYPE
     * Examples: USER_NOT_FOUND, VALIDATION_FAILED, INSTAGRAM_API_ERROR
     *
     * @return unique error code string
     */
    String getCode();

    /**
     * Get the human-readable error message.
     * Should be clear and actionable for API consumers.
     *
     * @return error message
     */
    String getMessage();

    /**
     * Get the HTTP status code associated with this error.
     * This determines the HTTP response status when this error occurs.
     *
     * @return HTTP status code
     */
    HttpStatus getHttpStatus();
}
