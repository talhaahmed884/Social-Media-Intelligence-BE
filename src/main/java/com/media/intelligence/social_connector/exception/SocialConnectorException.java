package com.media.intelligence.social_connector.exception;

import lombok.Getter;

/**
 * Domain-specific exception for Social Connector operations.
 * <p>
 * Wraps all errors that occur during social media platform integration,
 * providing consistent error handling across adapters.
 * <p>
 * **Exception Translation Pattern**: Translates platform-specific exceptions
 * into our domain exception, maintaining the Anti-Corruption Layer.
 */
@Getter
public class SocialConnectorException extends RuntimeException {

    private final SocialConnectorErrorCode errorCode;
    private final String details;

    /**
     * Create exception with error code only.
     *
     * @param errorCode specific error code
     */
    public SocialConnectorException(SocialConnectorErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.details = null;
    }

    /**
     * Create exception with error code and additional details.
     *
     * @param errorCode specific error code
     * @param details   additional error context
     */
    public SocialConnectorException(SocialConnectorErrorCode errorCode, String details) {
        super(errorCode.getMessage() + ": " + details);
        this.errorCode = errorCode;
        this.details = details;
    }

    /**
     * Create exception with error code and root cause.
     *
     * @param errorCode specific error code
     * @param cause     underlying exception
     */
    public SocialConnectorException(SocialConnectorErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
        this.details = cause.getMessage();
    }

    /**
     * Create exception with error code, details, and root cause.
     *
     * @param errorCode specific error code
     * @param details   additional error context
     * @param cause     underlying exception
     */
    public SocialConnectorException(SocialConnectorErrorCode errorCode, String details, Throwable cause) {
        super(errorCode.getMessage() + ": " + details, cause);
        this.errorCode = errorCode;
        this.details = details;
    }

    /**
     * Get formatted error message including code.
     *
     * @return formatted error message
     */
    public String getFormattedMessage() {
        return String.format("[%s] %s%s",
                errorCode.getCode(),
                errorCode.getMessage(),
                details != null ? ": " + details : "");
    }
}
