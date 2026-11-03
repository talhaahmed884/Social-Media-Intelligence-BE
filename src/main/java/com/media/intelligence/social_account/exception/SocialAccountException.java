package com.media.intelligence.social_account.exception;

import com.media.intelligence.common.exception.BaseException;

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
}
