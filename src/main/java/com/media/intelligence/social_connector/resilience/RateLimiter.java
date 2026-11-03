package com.media.intelligence.social_connector.resilience;

import com.media.intelligence.social_connector.exception.SocialConnectorErrorCode;
import com.media.intelligence.social_connector.exception.SocialConnectorException;
import com.media.intelligence.social_connector.provider.SocialPlatform;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiter to prevent exceeding platform API rate limits.
 * <p>
 * **Strategy Pattern**: Different rate limiting strategies can be implemented
 * (token bucket, sliding window, etc.).
 * <p>
 * **Resilience Pattern**: Implements rate limiting to protect against
 * API quota exhaustion and service degradation.
 * <p>
 * Current implementation uses simple fixed-window rate limiting.
 */
@Slf4j
public class RateLimiter {

    // Rate limit configuration per platform (requests per minute)
    private static final Map<SocialPlatform, Integer> PLATFORM_LIMITS = Map.of(
            SocialPlatform.INSTAGRAM, 200,  // Instagram Graph API: 200 calls/hour
            SocialPlatform.TWITTER, 300,    // Twitter API v2: 300 calls/15min window
            SocialPlatform.FACEBOOK, 200,   // Facebook Graph API: 200 calls/hour
            SocialPlatform.LINKEDIN, 100    // LinkedIn API: 100 calls/day (conservative)
    );

    // Track request counts and reset times per platform
    private final Map<SocialPlatform, RequestTracker> trackers = new ConcurrentHashMap<>();

    /**
     * Check if a request can proceed without exceeding rate limit.
     * <p>
     * If limit would be exceeded, this method throws an exception.
     *
     * @param platform social media platform
     * @throws SocialConnectorException if rate limit exceeded
     */
    public void checkRateLimit(SocialPlatform platform) throws SocialConnectorException {
        RequestTracker tracker = trackers.computeIfAbsent(platform, RequestTracker::new);

        synchronized (tracker) {
            // Reset counter if window has expired
            if (LocalDateTime.now().isAfter(tracker.windowResetAt)) {
                tracker.reset();
            }

            // Check if limit exceeded
            if (tracker.requestCount >= tracker.limit) {
                long secondsUntilReset = Duration.between(
                        LocalDateTime.now(),
                        tracker.windowResetAt
                ).getSeconds();

                log.warn("Rate limit exceeded for {}: {} requests in window. " +
                                "Reset in {} seconds",
                        platform, tracker.requestCount, secondsUntilReset);

                throw new SocialConnectorException(
                        SocialConnectorErrorCode.RATE_LIMIT_EXCEEDED,
                        String.format("Rate limit for %s exceeded. Try again in %d seconds",
                                platform.getDisplayName(), secondsUntilReset)
                );
            }

            // Increment request count
            tracker.requestCount++;
            log.debug("Rate limit check for {}: {}/{} requests used",
                    platform, tracker.requestCount, tracker.limit);
        }
    }

    /**
     * Apply rate limiting delay (backoff) before making request.
     * <p>
     * This is a simple implementation using Thread.sleep.
     * For production, consider using:
     * - Resilience4j RateLimiter
     * - Bucket4j for token bucket algorithm
     * - Redis-based distributed rate limiting
     *
     * @param platform    social media platform
     * @param delayMillis delay in milliseconds
     */
    public void applyDelay(SocialPlatform platform, long delayMillis) {
        if (delayMillis > 0) {
            try {
                log.debug("Applying rate limit delay for {}: {}ms", platform, delayMillis);
                Thread.sleep(delayMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new SocialConnectorException(
                        SocialConnectorErrorCode.OPERATION_FAILED,
                        "Rate limit delay interrupted",
                        e
                );
            }
        }
    }

    /**
     * Get remaining requests for a platform in current window.
     *
     * @param platform social media platform
     * @return remaining requests
     */
    public int getRemainingRequests(SocialPlatform platform) {
        RequestTracker tracker = trackers.get(platform);
        if (tracker == null) {
            return PLATFORM_LIMITS.getOrDefault(platform, Integer.MAX_VALUE);
        }

        synchronized (tracker) {
            if (LocalDateTime.now().isAfter(tracker.windowResetAt)) {
                return tracker.limit;
            }
            return Math.max(0, tracker.limit - tracker.requestCount);
        }
    }

    /**
     * Reset rate limit tracking for a platform.
     * Useful for testing or manual reset.
     *
     * @param platform social media platform
     */
    public void reset(SocialPlatform platform) {
        RequestTracker tracker = trackers.get(platform);
        if (tracker != null) {
            synchronized (tracker) {
                tracker.reset();
            }
        }
    }

    /**
     * Internal class to track requests per platform.
     */
    private static class RequestTracker {
        private final SocialPlatform platform;
        private final int limit;
        private int requestCount;
        private LocalDateTime windowResetAt;

        RequestTracker(SocialPlatform platform) {
            this.platform = platform;
            this.limit = PLATFORM_LIMITS.getOrDefault(platform, 100);
            reset();
        }

        void reset() {
            this.requestCount = 0;
            // Reset window: 1 hour for most platforms
            this.windowResetAt = LocalDateTime.now().plusHours(1);
        }
    }
}
