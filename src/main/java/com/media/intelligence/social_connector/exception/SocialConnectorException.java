package com.media.intelligence.social_connector.exception;

import com.media.intelligence.common.exception.BaseException;
import lombok.Getter;

/**
 * Domain-specific exception for Social Connector operations.
 * <p>
 * Wraps all errors that occur during social media platform integration,
 * providing consistent error handling across adapters.
 * <p>
 * Extends {@link BaseException}, so the GlobalExceptionHandler returns the error code's
 * HTTP status and message. {@link #getDetails()} carries internal context for logs only
 * and is never sent to API clients.
 * <p>
 * **Exception Translation Pattern**: Translates platform-specific exceptions
 * into our domain exception, maintaining the Anti-Corruption Layer.
 */
public class SocialConnectorException extends BaseException {

    @Getter
    private final String details;

    /**
     * Create exception with error code only.
     *
     * @param errorCode specific error code
     */
    public SocialConnectorException(SocialConnectorErrorCode errorCode) {
        super(errorCode);
        this.details = null;
    }

    /**
     * Create exception with error code and additional details.
     *
     * @param errorCode specific error code
     * @param details   additional error context
     */
    public SocialConnectorException(SocialConnectorErrorCode errorCode, String details) {
        super(errorCode, buildMessage(errorCode, details), null);
        this.details = details;
    }

    /**
     * Create exception with error code and root cause.
     *
     * @param errorCode specific error code
     * @param cause     underlying exception
     */
    public SocialConnectorException(SocialConnectorErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
        this.details = cause != null ? cause.getMessage() : null;
    }

    /**
     * Create exception with error code, details, and root cause.
     *
     * @param errorCode specific error code
     * @param details   additional error context
     * @param cause     underlying exception
     */
    public SocialConnectorException(SocialConnectorErrorCode errorCode, String details, Throwable cause) {
        super(errorCode, buildMessage(errorCode, details), cause);
        this.details = details;
    }

    private static String buildMessage(SocialConnectorErrorCode errorCode, String details) {
        return details != null ? errorCode.getMessage() + ": " + details : errorCode.getMessage();
    }

    @Override
    public SocialConnectorErrorCode getErrorCode() {
        // Safe: every constructor only accepts SocialConnectorErrorCode
        return (SocialConnectorErrorCode) super.getErrorCode();
    }

    /**
     * Get formatted error message including code.
     *
     * @return formatted error message
     */
    public String getFormattedMessage() {
        return String.format("[%s] %s%s",
                getErrorCode().getCode(),
                getErrorCode().getMessage(),
                details != null ? ": " + details : "");
    }
}
