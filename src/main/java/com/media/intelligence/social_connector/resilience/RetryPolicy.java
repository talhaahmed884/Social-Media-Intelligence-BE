package com.media.intelligence.social_connector.resilience;

import com.media.intelligence.social_connector.exception.SocialConnectorErrorCode;
import com.media.intelligence.social_connector.exception.SocialConnectorException;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Supplier;

/**
 * Retry policy for transient failures during external API calls.
 * <p>
 * **Resilience Pattern**: Implements retry with exponential backoff
 * to handle transient network failures and temporary service unavailability.
 * <p>
 * Whether an error is retried is decided by {@link SocialConnectorErrorCode#isRetryable()}
 * (network errors and platform unavailability). Auth, rate-limit, not-found and other
 * client errors fail immediately.
 * <p>
 * For production use, consider:
 * - Resilience4j Retry
 * - Spring Retry (@Retryable)
 * - Circuit Breaker pattern for cascading failures
 */
@Slf4j
@Getter
@Builder
public class RetryPolicy {

    /**
     * Maximum number of attempts (including the first call)
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
     * Retries only {@link SocialConnectorException}s whose error code is retryable.
     * When attempts are exhausted the last exception is rethrown unchanged, so callers
     * see the real cause (e.g. PLATFORM_UNAVAILABLE). Other runtime exceptions are wrapped
     * in OPERATION_FAILED without retrying.
     *
     * @param operation operation to execute
     * @param <T>       return type
     * @return operation result
     * @throws SocialConnectorException if the operation fails
     */
    public <T> T execute(Supplier<T> operation) throws SocialConnectorException {
        int attempts = Math.max(1, maxAttempts);
        long delay = initialDelayMs;

        for (int attempt = 1; ; attempt++) {
            try {
                log.debug("Executing operation - attempt {}/{}", attempt, attempts);
                return operation.get();

            } catch (SocialConnectorException e) {
                if (!e.getErrorCode().isRetryable()) {
                    log.debug("Non-retryable error on attempt {}: {}", attempt, e.getMessage());
                    throw e;
                }

                if (attempt >= attempts) {
                    log.error("Operation failed after {} attempts: {}", attempts, e.getMessage());
                    throw e;
                }

                log.warn("Retryable error on attempt {}/{}: {}. Retrying in {}ms",
                        attempt, attempts, e.getMessage(), delay);

                sleep(delay);
                delay = Math.min((long) (delay * backoffMultiplier), maxDelayMs);

            } catch (RuntimeException e) {
                log.error("Unexpected error on attempt {}: {}", attempt, e.getMessage());
                throw new SocialConnectorException(
                        SocialConnectorErrorCode.OPERATION_FAILED,
                        "Unexpected error on attempt " + attempt,
                        e
                );
            }
        }
    }

    /**
     * Sleep for specified duration.
     *
     * @param millis milliseconds to sleep
     */
    private void sleep(long millis) {
        if (millis <= 0) {
            return;
        }
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
