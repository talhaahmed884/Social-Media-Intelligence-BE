package com.media.intelligence.social_connector.provider;

import com.media.intelligence.social_connector.exception.SocialConnectorErrorCode;
import com.media.intelligence.social_connector.exception.SocialConnectorException;

/**
 * Enum representing supported social media platforms.
 * <p>
 * This enum is used by the Factory pattern to instantiate
 * the appropriate adapter implementation.
 * <p>
 * Future platforms can be added here without modifying existing code
 * (Open/Closed Principle).
 */
public enum SocialPlatform {
    /**
     * Instagram platform - supports posts, comments, stories
     */
    INSTAGRAM("Instagram", "https://graph.instagram.com"),

    /**
     * Twitter/X platform - supports tweets, replies, retweets
     */
    TWITTER("Twitter", "https://api.twitter.com/2"),

    /**
     * Facebook platform - supports posts, comments, reactions
     */
    FACEBOOK("Facebook", "https://graph.facebook.com"),

    /**
     * LinkedIn platform - supports posts, comments, articles
     */
    LINKEDIN("LinkedIn", "https://api.linkedin.com/v2");

    private final String displayName;
    private final String baseApiUrl;

    SocialPlatform(String displayName, String baseApiUrl) {
        this.displayName = displayName;
        this.baseApiUrl = baseApiUrl;
    }

    /**
     * Get platform by display name (case-insensitive).
     *
     * @param name platform display name
     * @return SocialPlatform enum
     * @throws IllegalArgumentException if platform not found
     */
    public static SocialPlatform fromDisplayName(String name) {
        for (SocialPlatform platform : values()) {
            if (platform.displayName.equalsIgnoreCase(name)) {
                return platform;
            }
        }
        throw new IllegalArgumentException("Unknown social platform: " + name);
    }

    /**
     * Get platform from an API path/query value such as "instagram" (case-insensitive enum name).
     *
     * @param value platform name
     * @return SocialPlatform enum
     * @throws SocialConnectorException UNSUPPORTED_PLATFORM if no platform matches
     */
    public static SocialPlatform fromValue(String value) {
        for (SocialPlatform platform : values()) {
            if (platform.name().equalsIgnoreCase(value)) {
                return platform;
            }
        }
        throw new SocialConnectorException(
                SocialConnectorErrorCode.UNSUPPORTED_PLATFORM,
                "Unknown social platform: " + value
        );
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getBaseApiUrl() {
        return baseApiUrl;
    }
}
