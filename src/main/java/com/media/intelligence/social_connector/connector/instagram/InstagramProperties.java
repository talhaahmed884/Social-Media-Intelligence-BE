package com.media.intelligence.social_connector.connector.instagram;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.List;

/**
 * Instagram API with Instagram Login settings ({@code social-connector.instagram.*}).
 * <p>
 * {@code clientId}, {@code clientSecret} and {@code redirectUri} come from the Meta App Dashboard and are
 * only needed for the OAuth flow; fetching with an existing token works without them.
 * {@code redirectUri} must exactly match a URI registered in the dashboard.
 *
 * @param graphBaseUrl  Graph API host (token exchange/refresh use it unversioned)
 * @param apiVersion    Graph API version used for data endpoints
 * @param authorizeUrl  page the user is sent to for consent
 * @param tokenUrl      authorization-code exchange endpoint
 * @param clientId      Instagram App ID
 * @param clientSecret  Instagram App secret
 * @param redirectUri   OAuth redirect URI (our callback endpoint)
 * @param scopes        permissions requested during authorization
 * @param maxFetchLimit max items per request (comments edge returns at most 50)
 */
@ConfigurationProperties(prefix = "social-connector.instagram")
public record InstagramProperties(
        @DefaultValue("https://graph.instagram.com") String graphBaseUrl,
        @DefaultValue("v26.0") String apiVersion,
        @DefaultValue("https://www.instagram.com/oauth/authorize") String authorizeUrl,
        @DefaultValue("https://api.instagram.com/oauth/access_token") String tokenUrl,
        String clientId,
        String clientSecret,
        String redirectUri,
        @DefaultValue({"instagram_business_basic", "instagram_business_manage_comments"}) List<String> scopes,
        @DefaultValue("50") int maxFetchLimit
) {

    /**
     * @return base URL for versioned data endpoints, e.g. https://graph.instagram.com/v26.0
     */
    public String versionedBaseUrl() {
        return graphBaseUrl + "/" + apiVersion;
    }

    /**
     * @return true if the values needed for the OAuth flow are set
     */
    public boolean isOAuthConfigured() {
        return notBlank(clientId) && notBlank(clientSecret) && notBlank(redirectUri);
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * Never print the client secret.
     */
    @Override
    public String toString() {
        return "InstagramProperties[graphBaseUrl=" + graphBaseUrl + ", apiVersion=" + apiVersion
                + ", clientId=" + clientId + ", clientSecret=***, redirectUri=" + redirectUri
                + ", scopes=" + scopes + ", maxFetchLimit=" + maxFetchLimit + "]";
    }
}
