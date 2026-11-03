package com.media.intelligence.social_connector.exception;

/**
 * Error codes for Social Connector domain exceptions.
 * <p>
 * Provides specific error categorization for better error handling and logging.
 */
public enum SocialConnectorErrorCode {
    // Authentication Errors (1xxx)
    AUTHENTICATION_FAILED("SC1001", "Authentication with social platform failed"),
    INVALID_CREDENTIALS("SC1002", "Invalid or expired credentials"),
    TOKEN_EXPIRED("SC1003", "Access token has expired"),
    INSUFFICIENT_PERMISSIONS("SC1004", "Insufficient permissions for requested operation"),

    // Rate Limiting Errors (2xxx)
    RATE_LIMIT_EXCEEDED("SC2001", "Rate limit exceeded for platform API"),
    QUOTA_EXCEEDED("SC2002", "API quota exceeded"),

    // API Errors (3xxx)
    API_CALL_FAILED("SC3001", "External API call failed"),
    API_RESPONSE_INVALID("SC3002", "Invalid response from external API"),
    PARSE_ERROR("SC3003", "Failed to parse API response"),
    NETWORK_ERROR("SC3004", "Network error during API call"),

    // Resource Errors (4xxx)
    RESOURCE_NOT_FOUND("SC4001", "Requested resource not found"),
    POST_NOT_FOUND("SC4002", "Post not found"),
    COMMENT_NOT_FOUND("SC4003", "Comment not found"),
    USER_NOT_FOUND("SC4004", "User not found"),

    // Platform Errors (5xxx)
    UNSUPPORTED_PLATFORM("SC5001", "Social platform not supported"),
    PLATFORM_UNAVAILABLE("SC5002", "Social platform is currently unavailable"),
    PLATFORM_API_CHANGED("SC5003", "Platform API has changed unexpectedly"),

    // Data Errors (6xxx)
    DATA_SANITIZATION_FAILED("SC6001", "Failed to sanitize data"),
    DATA_VALIDATION_FAILED("SC6002", "Data validation failed"),

    // Configuration Errors (7xxx)
    CONFIGURATION_ERROR("SC7001", "Connector configuration error"),
    MISSING_CONFIGURATION("SC7002", "Required configuration missing"),

    // General Errors (9xxx)
    UNKNOWN_ERROR("SC9001", "Unknown error occurred"),
    OPERATION_FAILED("SC9002", "Operation failed");

    private final String code;
    private final String message;

    SocialConnectorErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return code + ": " + message;
    }
}
