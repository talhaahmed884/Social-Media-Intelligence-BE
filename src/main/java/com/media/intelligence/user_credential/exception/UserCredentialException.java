package com.media.intelligence.user_credential.exception;

import com.media.intelligence.common.exception.BaseException;

import java.util.List;

/**
 * Exception for user credential-related errors.
 */
public class UserCredentialException extends BaseException {
    public UserCredentialException(UserCredentialErrorCode errorCode) {
        super(errorCode);
    }

    public UserCredentialException(UserCredentialErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    /**
     * Create an exception carrying client-facing details (e.g. field validation errors).
     */
    public UserCredentialException(UserCredentialErrorCode errorCode, List<String> errors) {
        super(errorCode, errors);
    }
}
