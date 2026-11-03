package com.media.intelligence.social_connector.dto;

import com.media.intelligence.social_connector.provider.SocialPlatform;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Credentials for one linked social account, passed to every fetch call.
 * <p>
 * Connectors are stateless singletons, so the caller (e.g. a future social-account service that
 * loads tokens from the database) supplies the credentials per request. This lets one connector
 * instance serve any number of users and accounts concurrently.
 *
 * @param platform    platform the token belongs to
 * @param accountId   platform account ID (also the rate-limit key)
 * @param accessToken platform access token
 * @param expiresAt   token expiry in UTC, or null if unknown
 */
public record SocialCredentials(
        SocialPlatform platform,
        String accountId,
        String accessToken,
        LocalDateTime expiresAt
) {

    /**
     * Build credentials from a successful {@code connect()} response.
     */
    public static SocialCredentials from(SocialAuthResponse authResponse) {
        return new SocialCredentials(
                authResponse.getPlatform(),
                authResponse.getUserId(),
                authResponse.getAccessToken(),
                authResponse.getTokenExpiresAt()
        );
    }

    /**
     * @return true if an expiry is known and has passed
     */
    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(LocalDateTime.now(ZoneOffset.UTC));
    }

    /**
     * Never print the access token.
     */
    @Override
    public String toString() {
        return "SocialCredentials[platform=" + platform + ", accountId=" + accountId
                + ", accessToken=***, expiresAt=" + expiresAt + "]";
    }
}
