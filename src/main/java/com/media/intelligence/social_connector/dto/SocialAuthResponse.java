package com.media.intelligence.social_connector.dto;

import com.media.intelligence.social_connector.provider.SocialPlatform;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * DTO for social media authentication responses.
 * <p>
 * Contains authentication status, tokens, and user information
 * returned after successful connection.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SocialAuthResponse {

    /**
     * Authentication success status
     */
    private boolean authenticated;

    /**
     * Social media platform
     */
    private SocialPlatform platform;

    /**
     * Access token (may be refreshed)
     */
    @ToString.Exclude
    private String accessToken;

    /**
     * Token expiration time (UTC), or null if the platform did not report it
     */
    private LocalDateTime tokenExpiresAt;

    /**
     * Authenticated user's platform ID
     */
    private String userId;

    /**
     * Authenticated user's username/handle
     */
    private String username;

    /**
     * User's display name
     */
    private String displayName;

    /**
     * Profile picture URL
     */
    private String profilePictureUrl;

    /**
     * Rate limit information (calls remaining)
     */
    private Integer rateLimitRemaining;

    /**
     * Rate limit reset time
     */
    private LocalDateTime rateLimitResetAt;

    /**
     * Additional platform-specific metadata
     */
    private String metadata;
}
