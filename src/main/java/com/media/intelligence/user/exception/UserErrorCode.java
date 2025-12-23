package com.media.intelligence.user.exception;

import com.media.intelligence.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * Error codes specific to the User Domain.
 * Each error code includes a unique identifier, message, and HTTP status.
 * <p>
 * Code Format: USR_XXX where XXX is a unique number
 */
@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {
    // User Not Found Errors
    USER_NOT_FOUND("USR_001", "User not found", HttpStatus.NOT_FOUND),
    USER_ID_INVALID("USR_002", "Invalid user ID format", HttpStatus.BAD_REQUEST),

    // Email Related Errors
    EMAIL_ALREADY_EXISTS("USR_003", "Email is already registered", HttpStatus.CONFLICT),
    INVALID_EMAIL_FORMAT("USR_004", "Invalid email format", HttpStatus.BAD_REQUEST),

    // Registration Errors
    REGISTRATION_FAILED("USR_005", "User registration failed", HttpStatus.INTERNAL_SERVER_ERROR),
    WEAK_PASSWORD("USR_006", "Password does not meet security requirements", HttpStatus.BAD_REQUEST),
    INVALID_INPUT("USR_012", "Invalid input data", HttpStatus.BAD_REQUEST),

    // Authentication Errors
    INVALID_CREDENTIALS("USR_007", "Invalid email or password", HttpStatus.UNAUTHORIZED),
    ACCOUNT_DISABLED("USR_008", "User account is disabled", HttpStatus.FORBIDDEN),
    ACCOUNT_LOCKED("USR_009", "User account is locked", HttpStatus.FORBIDDEN),

    // Update Errors
    UPDATE_FAILED("USR_010", "Failed to update user information", HttpStatus.INTERNAL_SERVER_ERROR),
    CANNOT_DELETE_SELF("USR_011", "Cannot delete your own account", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
