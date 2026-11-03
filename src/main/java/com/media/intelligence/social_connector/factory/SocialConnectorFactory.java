package com.media.intelligence.social_connector.factory;

import com.media.intelligence.social_connector.connector.InstagramConnector;
import com.media.intelligence.social_connector.exception.SocialConnectorErrorCode;
import com.media.intelligence.social_connector.exception.SocialConnectorException;
import com.media.intelligence.social_connector.provider.SocialMediaProvider;
import com.media.intelligence.social_connector.provider.SocialPlatform;
import com.media.intelligence.social_connector.resilience.RateLimiter;
import com.media.intelligence.social_connector.resilience.RetryPolicy;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory for creating social media connector instances.
 * <p>
 * **Factory Pattern**: Encapsulates connector creation logic.
 * Clients request a connector by platform, and the factory returns
 * the appropriate implementation.
 * <p>
 * **Singleton Registry**: Maintains a registry of created connectors
 * to reuse instances (connection pooling pattern).
 * <p>
 * **Benefits**:
 * - Single point of connector creation
 * - Easy to add new platforms (just add a new case)
 * - Can apply configuration consistently
 * - Supports connector reuse (connection pooling)
 * <p>
 * **Usage**:
 * <pre>
 * SocialMediaProvider instagram = SocialConnectorFactory.createConnector(SocialPlatform.INSTAGRAM);
 * SocialMediaProvider twitter = SocialConnectorFactory.createConnector(SocialPlatform.TWITTER);
 * </pre>
 */
@Slf4j
public class SocialConnectorFactory {

    // Singleton instance
    private static final SocialConnectorFactory INSTANCE = new SocialConnectorFactory();

    // Shared resilience components (can be platform-specific if needed)
    private final RateLimiter sharedRateLimiter;
    private final RetryPolicy defaultRetryPolicy;

    // Registry of created connectors (for reuse/pooling)
    private final Map<SocialPlatform, SocialMediaProvider> connectorRegistry;

    /**
     * Private constructor (Singleton pattern).
     */
    private SocialConnectorFactory() {
        this.sharedRateLimiter = new RateLimiter();
        this.defaultRetryPolicy = RetryPolicy.defaultPolicy();
        this.connectorRegistry = new ConcurrentHashMap<>();
        log.debug("SocialConnectorFactory initialized");
    }

    /**
     * Get factory instance (Singleton).
     *
     * @return factory instance
     */
    public static SocialConnectorFactory getInstance() {
        return INSTANCE;
    }

    /**
     * Create connector for specified platform.
     * <p>
     * This is the main factory method - delegates to platform-specific methods.
     *
     * @param platform social media platform
     * @return configured connector instance
     * @throws SocialConnectorException if platform not supported
     */
    public static SocialMediaProvider createConnector(SocialPlatform platform)
            throws SocialConnectorException {
        return getInstance().createConnectorInstance(platform);
    }

    /**
     * Create connector with custom resilience configuration.
     *
     * @param platform    social media platform
     * @param rateLimiter custom rate limiter
     * @param retryPolicy custom retry policy
     * @return configured connector instance
     * @throws SocialConnectorException if platform not supported
     */
    public static SocialMediaProvider createConnector(
            SocialPlatform platform,
            RateLimiter rateLimiter,
            RetryPolicy retryPolicy) throws SocialConnectorException {
        return getInstance().createConnectorWithConfig(platform, rateLimiter, retryPolicy);
    }

    /**
     * Get or create cached connector instance.
     * <p>
     * **Connection Pooling**: Reuses existing connector if available.
     * Useful for maintaining persistent connections.
     *
     * @param platform social media platform
     * @return connector instance (cached or new)
     * @throws SocialConnectorException if platform not supported
     */
    public static SocialMediaProvider getOrCreateConnector(SocialPlatform platform)
            throws SocialConnectorException {
        return getInstance().getOrCreateCachedConnector(platform);
    }

    /**
     * Clear connector cache for a specific platform.
     * Forces creation of new connector on next request.
     *
     * @param platform social media platform
     */
    public static void clearConnector(SocialPlatform platform) {
        getInstance().removeCachedConnector(platform);
    }

    /**
     * Clear all cached connectors.
     */
    public static void clearAllConnectors() {
        getInstance().removeAllCachedConnectors();
    }

    // ==================== INSTANCE METHODS ====================

    /**
     * Start building connector with fluent API.
     *
     * @param platform social media platform
     * @return builder instance
     */
    public static ConnectorBuilder forPlatform(SocialPlatform platform) {
        return new ConnectorBuilder(platform);
    }

    /**
     * Create new connector instance (non-cached).
     */
    private SocialMediaProvider createConnectorInstance(SocialPlatform platform)
            throws SocialConnectorException {
        log.info("Creating new connector for platform: {}", platform.getDisplayName());

        return createConnectorWithConfig(platform, sharedRateLimiter, defaultRetryPolicy);
    }

    /**
     * Create connector with custom configuration.
     */
    private SocialMediaProvider createConnectorWithConfig(
            SocialPlatform platform,
            RateLimiter rateLimiter,
            RetryPolicy retryPolicy) throws SocialConnectorException {

        // Factory Method: Create appropriate connector based on platform
        SocialMediaProvider connector = switch (platform) {
            case INSTAGRAM -> new InstagramConnector(rateLimiter, retryPolicy);

            case TWITTER -> throw new SocialConnectorException(
                    SocialConnectorErrorCode.UNSUPPORTED_PLATFORM,
                    "Twitter connector not yet implemented"
            );
            // TODO: Implement TwitterConnector
            // case TWITTER -> new TwitterConnector(rateLimiter, retryPolicy);

            case FACEBOOK -> throw new SocialConnectorException(
                    SocialConnectorErrorCode.UNSUPPORTED_PLATFORM,
                    "Facebook connector not yet implemented"
            );
            // TODO: Implement FacebookConnector
            // case FACEBOOK -> new FacebookConnector(rateLimiter, retryPolicy);

            case LINKEDIN -> throw new SocialConnectorException(
                    SocialConnectorErrorCode.UNSUPPORTED_PLATFORM,
                    "LinkedIn connector not yet implemented"
            );
            // TODO: Implement LinkedInConnector
            // case LINKEDIN -> new LinkedInConnector(rateLimiter, retryPolicy);

            default -> throw new SocialConnectorException(
                    SocialConnectorErrorCode.UNSUPPORTED_PLATFORM,
                    "Platform not supported: " + platform.getDisplayName()
            );
        };

        log.info("Created connector for {}: {}", platform.getDisplayName(), connector.getClass().getSimpleName());
        return connector;
    }

    /**
     * Get or create cached connector (singleton per platform).
     */
    private SocialMediaProvider getOrCreateCachedConnector(SocialPlatform platform)
            throws SocialConnectorException {
        return connectorRegistry.computeIfAbsent(platform, p -> {
            log.debug("No cached connector for {}, creating new instance", p.getDisplayName());
            return createConnectorInstance(p);
        });
    }

    /**
     * Remove connector from cache.
     */
    private void removeCachedConnector(SocialPlatform platform) {
        SocialMediaProvider removed = connectorRegistry.remove(platform);
        if (removed != null) {
            log.info("Removed cached connector for {}", platform.getDisplayName());
            removed.disconnect(); // Cleanup connection
        }
    }

    // ==================== BUILDER METHODS (Alternative API) ====================

    /**
     * Remove all connectors from cache.
     */
    private void removeAllCachedConnectors() {
        log.info("Clearing all cached connectors ({} instances)", connectorRegistry.size());
        connectorRegistry.values().forEach(SocialMediaProvider::disconnect);
        connectorRegistry.clear();
    }

    /**
     * Fluent builder for connector creation.
     * <p>
     * **Builder Pattern**: Provides readable, flexible connector configuration.
     * <p>
     * Example:
     * <pre>
     * SocialMediaProvider connector = SocialConnectorFactory
     *     .forPlatform(SocialPlatform.INSTAGRAM)
     *     .withRetryPolicy(RetryPolicy.aggressivePolicy())
     *     .withRateLimiter(customLimiter)
     *     .cached()
     *     .build();
     * </pre>
     */
    public static class ConnectorBuilder {
        private final SocialPlatform platform;
        private RateLimiter rateLimiter;
        private RetryPolicy retryPolicy;
        private boolean cached = false;

        private ConnectorBuilder(SocialPlatform platform) {
            this.platform = platform;
        }

        /**
         * Set custom rate limiter.
         */
        public ConnectorBuilder withRateLimiter(RateLimiter rateLimiter) {
            this.rateLimiter = rateLimiter;
            return this;
        }

        /**
         * Set custom retry policy.
         */
        public ConnectorBuilder withRetryPolicy(RetryPolicy retryPolicy) {
            this.retryPolicy = retryPolicy;
            return this;
        }

        /**
         * Use cached connector instance (singleton per platform).
         */
        public ConnectorBuilder cached() {
            this.cached = true;
            return this;
        }

        /**
         * Build connector instance.
         */
        public SocialMediaProvider build() throws SocialConnectorException {
            SocialConnectorFactory factory = getInstance();

            if (cached) {
                // If custom configuration provided, can't use cached instance
                if (rateLimiter != null || retryPolicy != null) {
                    log.warn("Cannot use cached connector with custom configuration. Creating new instance.");
                    return factory.createConnectorWithConfig(
                            platform,
                            rateLimiter != null ? rateLimiter : factory.sharedRateLimiter,
                            retryPolicy != null ? retryPolicy : factory.defaultRetryPolicy
                    );
                }
                return factory.getOrCreateCachedConnector(platform);
            }

            // Create new instance
            return factory.createConnectorWithConfig(
                    platform,
                    rateLimiter != null ? rateLimiter : factory.sharedRateLimiter,
                    retryPolicy != null ? retryPolicy : factory.defaultRetryPolicy
            );
        }
    }
}
