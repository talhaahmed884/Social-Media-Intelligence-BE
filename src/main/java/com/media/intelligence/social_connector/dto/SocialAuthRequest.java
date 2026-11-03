package com.media.intelligence.social_connector.dto;

import com.media.intelligence.social_connector.provider.SocialPlatform;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * DTO for social media authentication requests.
 * <p>
 * Contains platform-specific credentials and configuration.
 * Different platforms may use different fields (OAuth tokens, API keys, etc.).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SocialAuthRequest {

    /**
     * Target social media platform
     */
    private SocialPlatform platform;

    /**
     * Access token (for OAuth 2.0 platforms)
     */
    @ToString.Exclude
    private String accessToken;

    /**
     * API key (for key-based authentication)
     */
    @ToString.Exclude
    private String apiKey;

    /**
     * API secret (for signed requests)
     */
    @ToString.Exclude
    private String apiSecret;

    /**
     * User ID or username (optional, platform-specific)
     */
    private String userId;

    /**
     * Additional platform-specific configuration
     * (stored as JSON string for flexibility)
     */
    private String additionalConfig;
}
