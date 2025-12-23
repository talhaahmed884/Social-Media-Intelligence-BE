package com.media.intelligence.user_credential.exception;

import com.media.intelligence.common.exception.BaseException;

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

    public UserCredentialException(UserCredentialErrorCode errorCode, Throwable cause, Object... args) {
        super(errorCode, cause);
    }
}
