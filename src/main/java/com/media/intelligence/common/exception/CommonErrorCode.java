package com.media.intelligence.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * Common error codes for validation and system-level errors.
 * These error codes are shared across all domains.
 * Each error code includes its HTTP status code for automatic response mapping.
 * <p>
 * Domain-specific error codes should be implemented in their respective domains:
 * - UserErrorCode (user domain)
 * - ConnectorErrorCode (social media integration)
 * - AnalysisErrorCode (LLM analysis domain)
 */
@Getter
@RequiredArgsConstructor
public enum CommonErrorCode implements ErrorCode {

    // Validation Errors (400 BAD_REQUEST)
    VALIDATION_FAILED("VALIDATION_FAILED", "Input validation failed", HttpStatus.BAD_REQUEST),
    INVALID_INPUT("INVALID_INPUT", "The provided input is invalid", HttpStatus.BAD_REQUEST),
    MISSING_REQUIRED_FIELD("MISSING_REQUIRED_FIELD", "A required field is missing", HttpStatus.BAD_REQUEST),

    // Resource Errors
    RESOURCE_NOT_FOUND("RESOURCE_NOT_FOUND", "The requested resource was not found", HttpStatus.NOT_FOUND),
    RESOURCE_ALREADY_EXISTS("RESOURCE_ALREADY_EXISTS", "The resource already exists", HttpStatus.CONFLICT),
    METHOD_NOT_ALLOWED("METHOD_NOT_ALLOWED", "HTTP method not supported for this endpoint", HttpStatus.METHOD_NOT_ALLOWED),

    // Authentication & Authorization
    UNAUTHORIZED("UNAUTHORIZED", "Authentication is required", HttpStatus.UNAUTHORIZED),
    FORBIDDEN("FORBIDDEN", "You do not have permission to access this resource", HttpStatus.FORBIDDEN),

    // System Errors
    INTERNAL_SERVER_ERROR("INTERNAL_SERVER_ERROR", "An unexpected error occurred", HttpStatus.INTERNAL_SERVER_ERROR),
    SERVICE_UNAVAILABLE("SERVICE_UNAVAILABLE", "The service is temporarily unavailable", HttpStatus.SERVICE_UNAVAILABLE),
    BAD_REQUEST("BAD_REQUEST", "The request is malformed or invalid", HttpStatus.BAD_REQUEST),
    MALFORMED_JSON("MALFORMED_JSON", "Malformed JSON request body", HttpStatus.BAD_REQUEST),
    MISSING_PARAMETER("MISSING_PARAMETER", "Required request parameter is missing", HttpStatus.BAD_REQUEST),

    // Database Errors
    DATABASE_ERROR("DATABASE_ERROR", "A database error occurred", HttpStatus.INTERNAL_SERVER_ERROR),
    CONSTRAINT_VIOLATION("CONSTRAINT_VIOLATION", "A database constraint was violated", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
