package com.media.intelligence.social_connector.resilience;

import com.media.intelligence.social_connector.exception.SocialConnectorErrorCode;
import com.media.intelligence.social_connector.exception.SocialConnectorException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RetryPolicy Tests")
public class RetryPolicyTest {

    private RetryPolicy retryPolicy;
    private AtomicInteger calls;

    @BeforeEach
    void setUp() {
        retryPolicy = RetryPolicy.builder()
                .maxAttempts(3)
                .initialDelayMs(0)
                .build();
        calls = new AtomicInteger();
    }

    @Test
    @DisplayName("execute: Should return result on first successful attempt")
    void execute_ShouldReturnResultOnFirstAttempt() {
        // Act
        String result = retryPolicy.execute(() -> {
            calls.incrementAndGet();
            return "ok";
        });

        // Assert
        assertEquals("ok", result);
        assertEquals(1, calls.get());
    }

    @Test
    @DisplayName("execute: Should retry retryable errors until success")
    void execute_ShouldRetryRetryableErrorsUntilSuccess() {
        // Act
        String result = retryPolicy.execute(() -> {
            if (calls.incrementAndGet() < 3) {
                throw new SocialConnectorException(SocialConnectorErrorCode.NETWORK_ERROR, "connection reset");
            }
            return "ok";
        });

        // Assert
        assertEquals("ok", result);
        assertEquals(3, calls.get());
    }

    @Test
    @DisplayName("execute: Should not retry non-retryable errors")
    void execute_ShouldNotRetryNonRetryableErrors() {
        // Act
        SocialConnectorException ex = assertThrows(SocialConnectorException.class, () ->
                retryPolicy.execute(() -> {
                    calls.incrementAndGet();
                    throw new SocialConnectorException(SocialConnectorErrorCode.AUTHENTICATION_FAILED, "bad token");
                }));

        // Assert
        assertEquals(SocialConnectorErrorCode.AUTHENTICATION_FAILED, ex.getErrorCode());
        assertEquals(1, calls.get());
    }

    @Test
    @DisplayName("execute: Should rethrow last error code when attempts are exhausted")
    void execute_ShouldRethrowLastErrorWhenExhausted() {
        // Act
        SocialConnectorException ex = assertThrows(SocialConnectorException.class, () ->
                retryPolicy.execute(() -> {
                    calls.incrementAndGet();
                    throw new SocialConnectorException(SocialConnectorErrorCode.PLATFORM_UNAVAILABLE, "503");
                }));

        // Assert
        assertEquals(SocialConnectorErrorCode.PLATFORM_UNAVAILABLE, ex.getErrorCode());
        assertEquals(3, calls.get());
    }

    @Test
    @DisplayName("execute: Should wrap unexpected runtime exceptions without retrying")
    void execute_ShouldWrapUnexpectedExceptions() {
        // Act
        SocialConnectorException ex = assertThrows(SocialConnectorException.class, () ->
                retryPolicy.execute(() -> {
                    calls.incrementAndGet();
                    throw new IllegalStateException("boom");
                }));

        // Assert
        assertEquals(SocialConnectorErrorCode.OPERATION_FAILED, ex.getErrorCode());
        assertInstanceOf(IllegalStateException.class, ex.getCause());
        assertEquals(1, calls.get());
    }

    @Test
    @DisplayName("execute: Should treat maxAttempts below 1 as a single attempt")
    void execute_ShouldTreatZeroMaxAttemptsAsOne() {
        // Arrange
        RetryPolicy zeroAttempts = RetryPolicy.builder().maxAttempts(0).initialDelayMs(0).build();

        // Act
        String result = zeroAttempts.execute(() -> {
            calls.incrementAndGet();
            return "ok";
        });

        // Assert
        assertEquals("ok", result);
        assertEquals(1, calls.get());
    }
}
