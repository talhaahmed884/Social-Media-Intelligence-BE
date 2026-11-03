package com.media.intelligence.user.exception;

import com.media.intelligence.common.exception.BaseException;

import java.util.List;

/**
 * User domain-specific exception.
 * Accepts UserErrorCode to define the specific error type.
 * <p>
 * The GlobalExceptionHandler will automatically catch this and extract
 * the error code, message, and HTTP status without needing user-specific handling.
 * <p>
 * Example usage:
 * <pre>
 * throw new UserException(UserErrorCode.EMAIL_ALREADY_EXISTS);
 * // Results in: 409 CONFLICT with error code "USR_003"
 * </pre>
 */
public class UserException extends BaseException {
    /**
     * Create a UserException with a specific error code.
     * HTTP status is automatically derived from the error code.
     *
     * @param errorCode the user-specific error code
     */
    public UserException(UserErrorCode errorCode) {
        super(errorCode);
    }

    /**
     * Create a UserException with a specific error code and underlying cause.
     *
     * @param errorCode the user-specific error code
     * @param cause     the underlying exception that caused this error
     */
    public UserException(UserErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    /**
     * Create a UserException carrying client-facing details (e.g. field validation errors).
     *
     * @param errorCode the user-specific error code
     * @param errors    detail messages returned in ErrorResponse.errors
     */
    public UserException(UserErrorCode errorCode, List<String> errors) {
        super(errorCode, errors);
    }
}
