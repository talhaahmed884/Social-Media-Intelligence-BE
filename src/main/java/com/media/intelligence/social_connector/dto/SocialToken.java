package com.media.intelligence.social_connector.dto;

import java.time.LocalDateTime;

/**
 * An access token issued or refreshed by a platform's OAuth endpoints.
 *
 * @param accessToken token value
 * @param expiresAt   expiry in UTC, or null if the platform did not report it
 */
public record SocialToken(String accessToken, LocalDateTime expiresAt) {

    /**
     * Never print the access token.
     */
    @Override
    public String toString() {
        return "SocialToken[accessToken=***, expiresAt=" + expiresAt + "]";
    }
}
