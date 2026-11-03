package com.media.intelligence.social_connector.resilience;

import com.media.intelligence.social_connector.exception.SocialConnectorErrorCode;
import com.media.intelligence.social_connector.exception.SocialConnectorException;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Supplier;

/**
 * Retry policy for transient failures during external API calls.
 * <p>
 * **Resilience Pattern**: Implements retry with exponential backoff
 * to handle transient network failures and temporary service unavailability.
 * <p>
 * For production use, consider:
 * - Resilience4j Retry
 * - Spring Retry (@Retryable)
 * - Circuit Breaker pattern for cascading failures
 */
@Slf4j
@Builder
public class RetryPolicy {

    /**
     * Maximum number of retry attempts
     */
    @Builder.Default
    private final int maxAttempts = 3;

    /**
     * Initial delay before first retry (milliseconds)
     */
    @Builder.Default
    private final long initialDelayMs = 1000;

    /**
     * Backoff multiplier for exponential backoff
     */
    @Builder.Default
    private final double backoffMultiplier = 2.0;

    /**
     * Maximum delay between retries (milliseconds)
     */
    @Builder.Default
    private final long maxDelayMs = 30000;

    /**
     * Create default retry policy with standard settings.
     *
     * @return configured retry policy
     */
    public static RetryPolicy defaultPolicy() {
        return RetryPolicy.builder().build();
    }

    /**
     * Create aggressive retry policy for critical operations.
     *
     * @return configured retry policy with more retries
     */
    public static RetryPolicy aggressivePolicy() {
        return RetryPolicy.builder()
                .maxAttempts(5)
                .initialDelayMs(500)
                .backoffMultiplier(2.0)
                .maxDelayMs(60000)
                .build();
    }

    /**
     * Create conservative retry policy for non-critical operations.
     *
     * @return configured retry policy with fewer retries
     */
    public static RetryPolicy conservativePolicy() {
        return RetryPolicy.builder()
                .maxAttempts(2)
                .initialDelayMs(2000)
                .backoffMultiplier(1.5)
                .maxDelayMs(15000)
                .build();
    }

    /**
     * Execute operation with retry logic.
     * <p>
     * Retries on transient failures (network errors, timeouts, 5xx errors).
     * Does NOT retry on client errors (4xx) or authentication failures.
     *
     * @param operation operation to execute
     * @param <T>       return type
     * @return operation result
     * @throws SocialConnectorException if all retries fail
     */
    public <T> T execute(Supplier<T> operation) throws SocialConnectorException {
        int attempt = 0;
        long delay = initialDelayMs;
        Throwable lastException = null;

        while (attempt < maxAttempts) {
            attempt++;

            try {
                log.debug("Executing operation - attempt {}/{}", attempt, maxAttempts);
                return operation.get();

            } catch (SocialConnectorException e) {
                lastException = e;

                // Don't retry on non-retryable errors
                if (!isRetryable(e.getErrorCode())) {
                    log.warn("Non-retryable error on attempt {}: {}", attempt, e.getMessage());
                    throw e;
                }

                // Don't retry if max attempts reached
                if (attempt >= maxAttempts) {
                    log.error("Max retry attempts ({}) exceeded", maxAttempts);
                    break;
                }

                // Log and wait before retry
                log.warn("Retryable error on attempt {}/{}: {}. Retrying in {}ms",
                        attempt, maxAttempts, e.getMessage(), delay);

                sleep(delay);

                // Exponential backoff
                delay = Math.min((long) (delay * backoffMultiplier), maxDelayMs);

            } catch (Exception e) {
                lastException = e;
                log.error("Unexpected error on attempt {}: {}", attempt, e.getMessage());

                // Wrap unexpected exceptions
                throw new SocialConnectorException(
                        SocialConnectorErrorCode.OPERATION_FAILED,
                        "Operation failed after " + attempt + " attempts",
                        e
                );
            }
        }

        // All retries failed
        log.error("Operation failed after {} attempts", maxAttempts);
        throw new SocialConnectorException(
                SocialConnectorErrorCode.OPERATION_FAILED,
                "Operation failed after " + maxAttempts + " retry attempts",
                lastException
        );
    }

    /**
     * Determine if an error is retryable.
     * <p>
     * Retryable errors:
     * - Network errors
     * - API call failures (temporary)
     * - Platform unavailable
     * <p>
     * Non-retryable errors:
     * - Authentication failures
     * - Invalid credentials
     * - Resource not found
     * - Rate limit exceeded (should use backoff instead)
     *
     * @param errorCode error code to check
     * @return true if retryable
     */
    private boolean isRetryable(SocialConnectorErrorCode errorCode) {
        return switch (errorCode) {
            // Retryable errors
            case NETWORK_ERROR,
                 API_CALL_FAILED,
                 PLATFORM_UNAVAILABLE,
                 UNKNOWN_ERROR -> true;

            // Non-retryable errors
            case AUTHENTICATION_FAILED,
                 INVALID_CREDENTIALS,
                 TOKEN_EXPIRED,
                 INSUFFICIENT_PERMISSIONS,
                 RATE_LIMIT_EXCEEDED,
                 QUOTA_EXCEEDED,
                 RESOURCE_NOT_FOUND,
                 POST_NOT_FOUND,
                 COMMENT_NOT_FOUND,
                 USER_NOT_FOUND,
                 UNSUPPORTED_PLATFORM,
                 DATA_VALIDATION_FAILED,
                 CONFIGURATION_ERROR,
                 MISSING_CONFIGURATION -> false;

            // Default: retry on unknown errors
            default -> {
                log.debug("Unknown error code {}, treating as retryable", errorCode);
                yield true;
            }
        };
    }

    /**
     * Sleep for specified duration.
     *
     * @param millis milliseconds to sleep
     */
    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SocialConnectorException(
                    SocialConnectorErrorCode.OPERATION_FAILED,
                    "Retry interrupted",
                    e
            );
        }
    }
}
