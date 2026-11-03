package com.media.intelligence.social_connector.resilience;

import com.media.intelligence.social_connector.config.SocialConnectorProperties;
import com.media.intelligence.social_connector.config.SocialConnectorProperties.RateLimit;
import com.media.intelligence.social_connector.exception.SocialConnectorErrorCode;
import com.media.intelligence.social_connector.exception.SocialConnectorException;
import com.media.intelligence.social_connector.provider.SocialPlatform;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Local rate limiter that guards against exceeding platform API rate limits.
 * <p>
 * **Resilience Pattern**: Fixed-window limiting per (platform, account), so one account's
 * traffic never blocks another's. Each platform has its own limit and window length,
 * overridable via {@code social-connector.rate-limits.<platform>}.
 * <p>
 * This is a conservative, in-memory, single-instance guard. Platforms still enforce their own
 * limits (surfaced as RATE_LIMIT_EXCEEDED by the connectors). For multiple app instances,
 * replace with a distributed limiter (e.g. Bucket4j + Redis).
 */
@Slf4j
@Component
public class RateLimiter {

    // Local guard limits per account. Instagram's real limit is 4800 * impressions per rolling 24h,
    // which cannot be computed locally; the others follow each platform's published defaults.
    private static final Map<SocialPlatform, RateLimit> DEFAULT_LIMITS = Map.of(
            SocialPlatform.INSTAGRAM, new RateLimit(200, Duration.ofHours(1)),
            SocialPlatform.TWITTER, new RateLimit(300, Duration.ofMinutes(15)),
            SocialPlatform.FACEBOOK, new RateLimit(200, Duration.ofHours(1)),
            SocialPlatform.LINKEDIN, new RateLimit(100, Duration.ofDays(1))
    );
    private static final RateLimit FALLBACK_LIMIT = new RateLimit(100, Duration.ofHours(1));

    private final Map<SocialPlatform, RateLimit> limits;
    private final Map<TrackerKey, RequestTracker> trackers = new ConcurrentHashMap<>();
    private final Clock clock;

    /**
     * Default limits and system clock.
     */
    public RateLimiter() {
        this(Map.of(), Clock.systemUTC());
    }

    /**
     * Default limits overridden by {@code social-connector.rate-limits}.
     */
    @Autowired
    public RateLimiter(SocialConnectorProperties properties) {
        this(properties.rateLimits(), Clock.systemUTC());
    }

    /**
     * Custom overrides and clock (for tests).
     */
    RateLimiter(Map<SocialPlatform, RateLimit> overrides, Clock clock) {
        Map<SocialPlatform, RateLimit> merged = new EnumMap<>(DEFAULT_LIMITS);
        merged.putAll(overrides);
        this.limits = Map.copyOf(merged);
        this.clock = clock;
    }

    /**
     * Record one request for an account, or throw if its window is exhausted.
     *
     * @param platform  social media platform
     * @param accountId platform account ID
     * @throws SocialConnectorException if rate limit exceeded
     */
    public void checkRateLimit(SocialPlatform platform, String accountId) throws SocialConnectorException {
        RequestTracker tracker = trackerFor(platform, accountId);

        synchronized (tracker) {
            Instant now = clock.instant();
            tracker.resetIfExpired(now);

            if (tracker.requestCount >= tracker.limit.maxRequests()) {
                long secondsUntilReset = Math.max(0, Duration.between(now, tracker.windowResetAt).getSeconds());

                log.warn("Rate limit exceeded for {} account {}: {} requests in window. Reset in {} seconds",
                        platform, accountId, tracker.requestCount, secondsUntilReset);

                throw new SocialConnectorException(
                        SocialConnectorErrorCode.RATE_LIMIT_EXCEEDED,
                        String.format("Rate limit for %s exceeded. Try again in %d seconds",
                                platform.getDisplayName(), secondsUntilReset)
                );
            }

            tracker.requestCount++;
            log.debug("Rate limit check for {} account {}: {}/{} requests used",
                    platform, accountId, tracker.requestCount, tracker.limit.maxRequests());
        }
    }

    /**
     * Get remaining requests for an account in the current window.
     *
     * @param platform  social media platform
     * @param accountId platform account ID
     * @return remaining requests
     */
    public int getRemainingRequests(SocialPlatform platform, String accountId) {
        RequestTracker tracker = trackers.get(new TrackerKey(platform, accountId));
        if (tracker == null) {
            return limitFor(platform).maxRequests();
        }

        synchronized (tracker) {
            tracker.resetIfExpired(clock.instant());
            return Math.max(0, tracker.limit.maxRequests() - tracker.requestCount);
        }
    }

    /**
     * Reset rate limit tracking for an account.
     *
     * @param platform  social media platform
     * @param accountId platform account ID
     */
    public void reset(SocialPlatform platform, String accountId) {
        trackers.remove(new TrackerKey(platform, accountId));
    }

    private RequestTracker trackerFor(SocialPlatform platform, String accountId) {
        return trackers.computeIfAbsent(new TrackerKey(platform, accountId),
                key -> new RequestTracker(limitFor(platform), clock.instant()));
    }

    private RateLimit limitFor(SocialPlatform platform) {
        return limits.getOrDefault(platform, FALLBACK_LIMIT);
    }

    private record TrackerKey(SocialPlatform platform, String accountId) {
    }

    /**
     * Mutable per-account counter; guarded by synchronizing on the instance.
     */
    private static class RequestTracker {
        private final RateLimit limit;
        private int requestCount;
        private Instant windowResetAt;

        RequestTracker(RateLimit limit, Instant now) {
            this.limit = limit;
            this.windowResetAt = now.plus(limit.window());
        }

        void resetIfExpired(Instant now) {
            if (!now.isBefore(windowResetAt)) {
                requestCount = 0;
                windowResetAt = now.plus(limit.window());
            }
        }
    }
}
