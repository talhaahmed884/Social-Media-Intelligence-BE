package com.media.intelligence.social_connector.provider;

import com.media.intelligence.social_connector.dto.SocialToken;
import com.media.intelligence.social_connector.exception.SocialConnectorException;

/**
 * Optional capability for connectors whose platform supports the OAuth authorization-code flow
 * and token refresh. Connectors implement it alongside {@link SocialMediaProvider};
 * {@code SocialConnectorFactory} discovers it automatically.
 */
public interface SocialOAuthProvider {

    /**
     * Get the platform this provider connects to.
     */
    SocialPlatform getPlatform();

    /**
     * Build the URL the user is sent to in order to grant access.
     *
     * @param state opaque value echoed back on the redirect (CSRF protection / user binding)
     * @return authorization URL
     * @throws SocialConnectorException if OAuth is not configured for this platform
     */
    String buildAuthorizationUrl(String state) throws SocialConnectorException;

    /**
     * Exchange the authorization code from the redirect for a token ready to store
     * (long-lived where the platform supports it).
     *
     * @param code authorization code
     * @return token with expiry
     * @throws SocialConnectorException if the exchange fails
     */
    SocialToken exchangeAuthorizationCode(String code) throws SocialConnectorException;

    /**
     * Refresh a still-valid token, extending its lifetime.
     *
     * @param accessToken current token
     * @return refreshed token with expiry
     * @throws SocialConnectorException if the refresh fails
     */
    SocialToken refreshToken(String accessToken) throws SocialConnectorException;
}
