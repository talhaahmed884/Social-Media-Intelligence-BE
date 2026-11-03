package com.media.intelligence.social_connector.exception;

import com.media.intelligence.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * Error codes for Social Connector domain exceptions.
 * <p>
 * Implements the common {@link ErrorCode} contract so the GlobalExceptionHandler maps these
 * to proper HTTP responses. Each code also declares whether it is safe to retry
 * (used by {@link com.media.intelligence.social_connector.resilience.RetryPolicy}).
 * <p>
 * Note: SC1xxx 401/403 responses mean the linked social account needs re-authorization,
 * not that the caller's session with this application is invalid.
 * <p>
 * Code Format: SCnxxx where n is the category
 */
@Getter
@RequiredArgsConstructor
public enum SocialConnectorErrorCode implements ErrorCode {
    // Authentication Errors (1xxx)
    AUTHENTICATION_FAILED("SC1001", "Authentication with social platform failed", HttpStatus.UNAUTHORIZED, false),
    INVALID_CREDENTIALS("SC1002", "Invalid or missing social platform credentials", HttpStatus.BAD_REQUEST, false),
    TOKEN_EXPIRED("SC1003", "Access token has expired", HttpStatus.UNAUTHORIZED, false),
    INSUFFICIENT_PERMISSIONS("SC1004", "Insufficient permissions for requested operation", HttpStatus.FORBIDDEN, false),

    // Rate Limiting Errors (2xxx)
    RATE_LIMIT_EXCEEDED("SC2001", "Rate limit exceeded for platform API", HttpStatus.TOO_MANY_REQUESTS, false),
    QUOTA_EXCEEDED("SC2002", "API quota exceeded", HttpStatus.TOO_MANY_REQUESTS, false),

    // API Errors (3xxx)
    API_CALL_FAILED("SC3001", "External API call failed", HttpStatus.BAD_GATEWAY, false),
    API_RESPONSE_INVALID("SC3002", "Invalid response from external API", HttpStatus.BAD_GATEWAY, false),
    PARSE_ERROR("SC3003", "Failed to parse API response", HttpStatus.BAD_GATEWAY, false),
    NETWORK_ERROR("SC3004", "Network error during API call", HttpStatus.SERVICE_UNAVAILABLE, true),

    // Resource Errors (4xxx)
    RESOURCE_NOT_FOUND("SC4001", "Requested resource not found", HttpStatus.NOT_FOUND, false),
    POST_NOT_FOUND("SC4002", "Post not found", HttpStatus.NOT_FOUND, false),
    COMMENT_NOT_FOUND("SC4003", "Comment not found", HttpStatus.NOT_FOUND, false),
    USER_NOT_FOUND("SC4004", "User not found", HttpStatus.NOT_FOUND, false),

    // Platform Errors (5xxx)
    UNSUPPORTED_PLATFORM("SC5001", "Social platform not supported", HttpStatus.BAD_REQUEST, false),
    PLATFORM_UNAVAILABLE("SC5002", "Social platform is currently unavailable", HttpStatus.SERVICE_UNAVAILABLE, true),
    PLATFORM_API_CHANGED("SC5003", "Platform API has changed unexpectedly", HttpStatus.BAD_GATEWAY, false),

    // Data Errors (6xxx)
    DATA_SANITIZATION_FAILED("SC6001", "Failed to sanitize data", HttpStatus.INTERNAL_SERVER_ERROR, false),
    DATA_VALIDATION_FAILED("SC6002", "Data validation failed", HttpStatus.BAD_REQUEST, false),

    // Configuration Errors (7xxx)
    CONFIGURATION_ERROR("SC7001", "Connector configuration error", HttpStatus.INTERNAL_SERVER_ERROR, false),
    MISSING_CONFIGURATION("SC7002", "Required configuration missing", HttpStatus.INTERNAL_SERVER_ERROR, false),

    // General Errors (9xxx)
    UNKNOWN_ERROR("SC9001", "Unknown error occurred", HttpStatus.INTERNAL_SERVER_ERROR, false),
    OPERATION_FAILED("SC9002", "Operation failed", HttpStatus.INTERNAL_SERVER_ERROR, false);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
    private final boolean retryable;
}
