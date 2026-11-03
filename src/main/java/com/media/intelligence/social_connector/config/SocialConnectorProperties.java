package com.media.intelligence.social_connector.config;

import com.media.intelligence.social_connector.provider.SocialPlatform;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;
import java.util.Map;

/**
 * Settings shared by all social connectors ({@code social-connector.*} in application.yml).
 * Platform-specific settings live next to each platform (e.g. {@code InstagramProperties}).
 *
 * @param http       HTTP timeouts
 * @param retry      retry policy for transient platform errors
 * @param rateLimits optional per-platform overrides of the local rate-limit guard
 */
@ConfigurationProperties(prefix = "social-connector")
public record SocialConnectorProperties(
        @DefaultValue Http http,
        @DefaultValue Retry retry,
        Map<SocialPlatform, RateLimit> rateLimits
) {

    public SocialConnectorProperties {
        rateLimits = rateLimits != null ? Map.copyOf(rateLimits) : Map.of();
    }

    /**
     * @param connectTimeout TCP connect timeout
     * @param readTimeout    time to wait for a response
     */
    public record Http(
            @DefaultValue("10s") Duration connectTimeout,
            @DefaultValue("30s") Duration readTimeout
    ) {
    }

    /**
     * @param maxAttempts       attempts including the first call
     * @param initialDelay      delay before the first retry
     * @param backoffMultiplier multiplier applied to the delay after each retry
     * @param maxDelay          upper bound for the delay
     */
    public record Retry(
            @DefaultValue("3") int maxAttempts,
            @DefaultValue("1s") Duration initialDelay,
            @DefaultValue("2.0") double backoffMultiplier,
            @DefaultValue("30s") Duration maxDelay
    ) {
    }

    /**
     * @param maxRequests requests allowed per window per account
     * @param window      fixed window length
     */
    public record RateLimit(int maxRequests, Duration window) {
    }
}
