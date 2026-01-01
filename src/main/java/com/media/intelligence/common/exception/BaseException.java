package com.media.intelligence.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Abstract base exception that holds an ErrorCode.
 * All domain-specific exceptions should extend this class.
 * <p>
 * The ErrorCode interface allows the GlobalExceptionHandler to dynamically
 * extract error codes, messages, and HTTP status without knowing the specific exception type.
 * <p>
 * Example usage:
 * <pre>
 * public class UserNotFoundException extends BaseException {
 *     public UserNotFoundException(String userId) {
 *         super(UserErrorCode.USER_NOT_FOUND);  // Status comes from ErrorCode
 *     }
 * }
 * </pre>
 */
@Getter
public abstract class BaseException extends RuntimeException {
    private final ErrorCode errorCode;
    private final HttpStatus httpStatus;

    /**
     * Constructor with ErrorCode.
     * HTTP status is automatically derived from errorCode.getHttpStatus()
     *
     * @param errorCode the error code defining the error type
     */
    protected BaseException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.httpStatus = errorCode.getHttpStatus();
    }

    /**
     * Constructor with ErrorCode and explicit HTTP status override.
     * Use this only when you need to override the default status from ErrorCode.
     *
     * @param errorCode  the error code defining the error type
     * @param httpStatus the HTTP status to return (overrides errorCode.getHttpStatus())
     */
    protected BaseException(ErrorCode errorCode, HttpStatus httpStatus) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    /**
     * Constructor with ErrorCode and cause.
     * HTTP status is automatically derived from errorCode.getHttpStatus()
     *
     * @param errorCode the error code defining the error type
     * @param cause     the underlying cause of this exception
     */
    protected BaseException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
        this.httpStatus = errorCode.getHttpStatus();
    }

    /**
     * Constructor with ErrorCode, explicit HTTP status, and cause.
     * Use this only when you need to override the default status from ErrorCode.
     *
     * @param errorCode  the error code defining the error type
     * @param httpStatus the HTTP status to return (overrides errorCode.getHttpStatus())
     * @param cause      the underlying cause of this exception
     */
    protected BaseException(ErrorCode errorCode, HttpStatus httpStatus, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
}
