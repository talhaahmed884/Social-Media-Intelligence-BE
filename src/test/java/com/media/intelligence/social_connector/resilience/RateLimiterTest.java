package com.media.intelligence.social_connector.resilience;

import com.media.intelligence.social_connector.exception.SocialConnectorErrorCode;
import com.media.intelligence.social_connector.exception.SocialConnectorException;
import com.media.intelligence.social_connector.provider.SocialPlatform;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RateLimiter Tests")
public class RateLimiterTest {

    private static final int INSTAGRAM_LIMIT = 200;
    private static final int LINKEDIN_LIMIT = 100;

    private MutableClock clock;
    private RateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
        rateLimiter = new RateLimiter(Map.of(), clock);
    }

    @Test
    @DisplayName("checkRateLimit: Should allow requests up to the limit, then throw")
    void checkRateLimit_ShouldThrowAfterLimit() {
        // Arrange
        exhaust(SocialPlatform.INSTAGRAM, "account-a", INSTAGRAM_LIMIT);

        // Act
        SocialConnectorException ex = assertThrows(SocialConnectorException.class, () ->
                rateLimiter.checkRateLimit(SocialPlatform.INSTAGRAM, "account-a"));

        // Assert
        assertEquals(SocialConnectorErrorCode.RATE_LIMIT_EXCEEDED, ex.getErrorCode());
        assertEquals(0, rateLimiter.getRemainingRequests(SocialPlatform.INSTAGRAM, "account-a"));
    }

    @Test
    @DisplayName("checkRateLimit: Should track each account independently")
    void checkRateLimit_ShouldTrackAccountsIndependently() {
        // Arrange
        exhaust(SocialPlatform.INSTAGRAM, "account-a", INSTAGRAM_LIMIT);

        // Act & Assert
        assertDoesNotThrow(() -> rateLimiter.checkRateLimit(SocialPlatform.INSTAGRAM, "account-b"));
        assertEquals(INSTAGRAM_LIMIT - 1, rateLimiter.getRemainingRequests(SocialPlatform.INSTAGRAM, "account-b"));
    }

    @Test
    @DisplayName("checkRateLimit: Should reset after the platform window passes")
    void checkRateLimit_ShouldResetAfterWindow() {
        // Arrange
        exhaust(SocialPlatform.INSTAGRAM, "account-a", INSTAGRAM_LIMIT);

        // Act
        clock.advance(Duration.ofHours(1));

        // Assert
        assertEquals(INSTAGRAM_LIMIT, rateLimiter.getRemainingRequests(SocialPlatform.INSTAGRAM, "account-a"));
        assertDoesNotThrow(() -> rateLimiter.checkRateLimit(SocialPlatform.INSTAGRAM, "account-a"));
    }

    @Test
    @DisplayName("checkRateLimit: Should use a daily window for LinkedIn")
    void checkRateLimit_ShouldUseDailyWindowForLinkedIn() {
        // Arrange
        exhaust(SocialPlatform.LINKEDIN, "account-a", LINKEDIN_LIMIT);

        // Act & Assert: still blocked after one hour
        clock.advance(Duration.ofHours(1));
        assertThrows(SocialConnectorException.class, () ->
                rateLimiter.checkRateLimit(SocialPlatform.LINKEDIN, "account-a"));

        // Act & Assert: allowed after one day
        clock.advance(Duration.ofHours(23));
        assertDoesNotThrow(() -> rateLimiter.checkRateLimit(SocialPlatform.LINKEDIN, "account-a"));
    }

    @Test
    @DisplayName("getRemainingRequests: Should return full limit for unknown accounts")
    void getRemainingRequests_ShouldReturnFullLimitForUnknownAccount() {
        assertEquals(INSTAGRAM_LIMIT, rateLimiter.getRemainingRequests(SocialPlatform.INSTAGRAM, "new-account"));
    }

    @Test
    @DisplayName("reset: Should clear tracking for an account")
    void reset_ShouldClearTracking() {
        // Arrange
        exhaust(SocialPlatform.INSTAGRAM, "account-a", INSTAGRAM_LIMIT);

        // Act
        rateLimiter.reset(SocialPlatform.INSTAGRAM, "account-a");

        // Assert
        assertEquals(INSTAGRAM_LIMIT, rateLimiter.getRemainingRequests(SocialPlatform.INSTAGRAM, "account-a"));
    }

    private void exhaust(SocialPlatform platform, String accountId, int times) {
        for (int i = 0; i < times; i++) {
            rateLimiter.checkRateLimit(platform, accountId);
        }
    }

    /**
     * Clock whose time can be moved forward in tests.
     */
    private static class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant start) {
            this.now = start;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
