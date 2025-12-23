package com.media.intelligence.user_credential.exception;

import com.media.intelligence.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * Error codes specific to the User Credential Domain.
 * Each error code includes a unique identifier, message template, and HTTP status.
 * <p>
 * Code Format: CRED_XXX where XXX is a unique number
 */
@Getter
@RequiredArgsConstructor
public enum UserCredentialErrorCode implements ErrorCode {

    CREDENTIAL_NOT_FOUND("CRED_001", "Credentials not found for user", HttpStatus.NOT_FOUND),
    INVALID_PASSWORD("CRED_002", "Invalid password provided", HttpStatus.BAD_REQUEST),
    PASSWORD_HASH_FAILED("CRED_003", "Failed to hash password", HttpStatus.INTERNAL_SERVER_ERROR),
    PASSWORD_VERIFICATION_FAILED("CRED_004", "Failed to verify password", HttpStatus.INTERNAL_SERVER_ERROR),
    CREDENTIAL_CREATION_FAILED("CRED_005", "Failed to create credentials", HttpStatus.INTERNAL_SERVER_ERROR),
    CREDENTIAL_UPDATE_FAILED("CRED_006", "Failed to update credentials", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_ALGORITHM("CRED_007", "Invalid hashing algorithm", HttpStatus.BAD_REQUEST),
    PASSWORD_REQUIRED("CRED_008", "Password is required", HttpStatus.BAD_REQUEST),
    PASSWORD_HASH_EMPTY("CRED_009", "Password hash cannot be null or empty", HttpStatus.BAD_REQUEST),
    INVALID_PASSWORD_HASH("CRED_010", "Invalid password hash", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
