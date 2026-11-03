package com.media.intelligence.social_connector.exception;

import com.media.intelligence.common.dto.error_response.ErrorResponse;
import com.media.intelligence.common.exception.BaseException;
import com.media.intelligence.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SocialConnectorException Tests")
public class SocialConnectorExceptionTest {

    @Test
    @DisplayName("SocialConnectorException: Should be a BaseException with status from its error code")
    void exception_ShouldCarryHttpStatusFromErrorCode() {
        // Act
        SocialConnectorException ex = new SocialConnectorException(
                SocialConnectorErrorCode.RATE_LIMIT_EXCEEDED, "retry in 30 seconds");

        // Assert
        assertInstanceOf(BaseException.class, ex);
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.getHttpStatus());
        assertEquals("retry in 30 seconds", ex.getDetails());
        assertTrue(ex.getMessage().contains("retry in 30 seconds"));
        assertEquals("[SC2001] Rate limit exceeded for platform API: retry in 30 seconds", ex.getFormattedMessage());
    }

    @Test
    @DisplayName("GlobalExceptionHandler: Should return error code and public message, not internal details")
    void globalExceptionHandler_ShouldMapSocialConnectorException() {
        // Arrange
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        SocialConnectorException ex = new SocialConnectorException(
                SocialConnectorErrorCode.PLATFORM_UNAVAILABLE, "Instagram 'me' returned HTTP 503");

        // Act
        ResponseEntity<ErrorResponse> response = handler.handleBaseException(ex, null);

        // Assert
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("SC5002", response.getBody().getErrorCode());
        assertEquals(SocialConnectorErrorCode.PLATFORM_UNAVAILABLE.getMessage(), response.getBody().getMessage());
    }

    @Test
    @DisplayName("SocialConnectorErrorCode: Should only mark transient errors as retryable")
    void errorCode_ShouldMarkOnlyTransientErrorsRetryable() {
        for (SocialConnectorErrorCode code : SocialConnectorErrorCode.values()) {
            boolean expected = code == SocialConnectorErrorCode.NETWORK_ERROR
                    || code == SocialConnectorErrorCode.PLATFORM_UNAVAILABLE;
            assertEquals(expected, code.isRetryable(), code.name());
        }
    }
}
