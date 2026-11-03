package com.media.intelligence.social_account.exception;

import com.media.intelligence.common.exception.BaseException;

import java.util.List;

/**
 * Social account domain-specific exception.
 * The GlobalExceptionHandler maps it using the error code's HTTP status and message.
 */
public class SocialAccountException extends BaseException {
    public SocialAccountException(SocialAccountErrorCode errorCode) {
        super(errorCode);
    }

    public SocialAccountException(SocialAccountErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    /**
     * Create an exception carrying client-facing details (e.g. field validation errors).
     */
    public SocialAccountException(SocialAccountErrorCode errorCode, List<String> errors) {
        super(errorCode, errors);
    }
}
