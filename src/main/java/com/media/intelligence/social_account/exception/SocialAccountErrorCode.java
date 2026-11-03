package com.media.intelligence.social_account.exception;

import com.media.intelligence.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * Error codes specific to the Social Account Domain.
 * <p>
 * Platform/connector failures use {@code SocialConnectorErrorCode} (SCnxxx) instead.
 * <p>
 * Code Format: SACC_XXX where XXX is a unique number
 */
@Getter
@RequiredArgsConstructor
public enum SocialAccountErrorCode implements ErrorCode {
    SOCIAL_ACCOUNT_NOT_FOUND("SACC_001", "Social account not found", HttpStatus.NOT_FOUND),
    INVALID_INPUT("SACC_002", "Invalid input data", HttpStatus.BAD_REQUEST),
    INVALID_OAUTH_STATE("SACC_003", "Authorization link is invalid or has expired", HttpStatus.BAD_REQUEST),
    OAUTH_AUTHORIZATION_DENIED("SACC_004", "Authorization was denied on the social platform", HttpStatus.BAD_REQUEST),
    TOKEN_ENCRYPTION_NOT_CONFIGURED("SACC_005", "Token encryption is not configured", HttpStatus.INTERNAL_SERVER_ERROR),
    TOKEN_ENCRYPTION_FAILED("SACC_006", "Failed to encrypt or decrypt access token", HttpStatus.INTERNAL_SERVER_ERROR),
    REAUTHORIZATION_REQUIRED("SACC_007", "Social account must be re-linked", HttpStatus.CONFLICT);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
