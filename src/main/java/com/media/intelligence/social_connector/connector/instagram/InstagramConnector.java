package com.media.intelligence.social_connector.connector.instagram;

import com.fasterxml.jackson.databind.JsonNode;
import com.media.intelligence.social_connector.connector.AbstractSocialConnector;
import com.media.intelligence.social_connector.dto.SocialAuthRequest;
import com.media.intelligence.social_connector.dto.SocialAuthResponse;
import com.media.intelligence.social_connector.dto.SocialComment;
import com.media.intelligence.social_connector.dto.SocialCredentials;
import com.media.intelligence.social_connector.dto.SocialPost;
import com.media.intelligence.social_connector.dto.SocialToken;
import com.media.intelligence.social_connector.exception.SocialConnectorErrorCode;
import com.media.intelligence.social_connector.exception.SocialConnectorException;
import com.media.intelligence.social_connector.provider.SocialOAuthProvider;
import com.media.intelligence.social_connector.provider.SocialPlatform;
import com.media.intelligence.social_connector.resilience.RateLimiter;
import com.media.intelligence.social_connector.resilience.RetryPolicy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Instagram API (with Instagram Login) adapter.
 * <p>
 * **Adapter Pattern**: Adapts the Instagram Graph API to {@link com.media.intelligence.social_connector.provider.SocialMediaProvider}
 * and {@link SocialOAuthProvider}. This class only orchestrates; HTTP lives in {@link InstagramApiClient},
 * JSON mapping in {@link InstagramResponseParser}, error mapping in {@link InstagramErrorMapper},
 * settings in {@link InstagramProperties}.
 * <p>
 * **API Documentation**:
 * https://developers.facebook.com/docs/instagram-platform/instagram-api-with-instagram-login
 * <p>
 * **OAuth flow** (Business Login for Instagram):
 * authorize URL → redirect with {@code code} (valid 1h, single use) → POST code exchange
 * (short-lived token, 1h) → {@code ig_exchange_token} (long-lived, 60 days) →
 * {@code ig_refresh_token} (token must be ≥ 24h old and still valid).
 */
@Slf4j
@Component
public class InstagramConnector extends AbstractSocialConnector<JsonNode> implements SocialOAuthProvider {

    private static final String PROFILE_FIELDS = "user_id,username,account_type";
    private static final String COMMENT_FIELDS = "id,text,username,timestamp,like_count";
    // TODO: Meta's IG Media reference marks `caption` as Facebook-Login only; verify against the live API
    private static final String MEDIA_FIELDS =
            "id,caption,media_type,media_url,permalink,timestamp,like_count,comments_count,owner,username";

    private final InstagramApiClient apiClient;
    private final InstagramProperties properties;
    private final Clock clock;

    public InstagramConnector(RateLimiter rateLimiter,
                              RetryPolicy retryPolicy,
                              InstagramApiClient apiClient,
                              InstagramProperties properties,
                              Clock clock) {
        super(rateLimiter, retryPolicy);
        this.apiClient = apiClient;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public SocialPlatform getPlatform() {
        return SocialPlatform.INSTAGRAM;
    }

    @Override
    protected int getMaxFetchLimit() {
        return properties.maxFetchLimit();
    }

    // ==================== AUTHENTICATION ====================

    @Override
    protected SocialAuthResponse authenticateWithPlatform(SocialAuthRequest authRequest)
            throws SocialConnectorException {
        String accessToken = authRequest.getAccessToken();
        if (isBlank(accessToken)) {
            throw new SocialConnectorException(
                    SocialConnectorErrorCode.INVALID_CREDENTIALS,
                    "Instagram requires an access token for authentication"
            );
        }

        JsonNode profile = apiClient.getGraph("profile lookup", List.of("me"),
                Map.of("fields", PROFILE_FIELDS), accessToken);

        SocialAuthResponse response = InstagramResponseParser.parseProfile(profile, accessToken);
        response.setRateLimitRemaining(rateLimiter.getRemainingRequests(SocialPlatform.INSTAGRAM, response.getUserId()));
        return response;
    }

    // ==================== DATA API CALLS ====================

    @Override
    protected JsonNode callExternalApiForComments(SocialCredentials credentials, String postId, int limit) {
        return apiClient.getGraph("comments lookup", List.of(postId, "comments"),
                Map.of("fields", COMMENT_FIELDS, "limit", String.valueOf(limit)),
                credentials.accessToken());
    }

    @Override
    protected JsonNode callExternalApiForPosts(SocialCredentials credentials, String userId, int limit) {
        return apiClient.getGraph("media lookup", List.of(userId, "media"),
                Map.of("fields", MEDIA_FIELDS, "limit", String.valueOf(limit)),
                credentials.accessToken());
    }

    @Override
    protected JsonNode callExternalApiForPost(SocialCredentials credentials, String postId) {
        return apiClient.getGraph("post lookup", List.of(postId),
                Map.of("fields", MEDIA_FIELDS),
                credentials.accessToken());
    }

    @Override
    protected List<SocialComment> parseCommentsResponse(JsonNode rawResponse) {
        return InstagramResponseParser.parseComments(rawResponse);
    }

    @Override
    protected List<SocialPost> parsePostsResponse(JsonNode rawResponse) {
        return InstagramResponseParser.parsePosts(rawResponse);
    }

    @Override
    protected SocialPost parsePostResponse(JsonNode rawResponse) {
        return InstagramResponseParser.parsePost(rawResponse);
    }

    // ==================== OAUTH ====================

    @Override
    public String buildAuthorizationUrl(String state) throws SocialConnectorException {
        requireOAuthConfigured();

        return UriComponentsBuilder.fromUriString(properties.authorizeUrl())
                .queryParam("client_id", "{clientId}")
                .queryParam("redirect_uri", "{redirectUri}")
                .queryParam("response_type", "code")
                .queryParam("scope", "{scope}")
                .queryParam("state", "{state}")
                .encode()
                .buildAndExpand(Map.of(
                        "clientId", properties.clientId(),
                        "redirectUri", properties.redirectUri(),
                        "scope", String.join(",", properties.scopes()),
                        "state", state))
                .toUriString();
    }

    @Override
    public SocialToken exchangeAuthorizationCode(String code) throws SocialConnectorException {
        requireOAuthConfigured();
        if (isBlank(code)) {
            throw new SocialConnectorException(
                    SocialConnectorErrorCode.INVALID_CREDENTIALS,
                    "Authorization code is required"
            );
        }

        // Instagram appends "#_" to the redirect; it is not part of the code
        String cleanCode = code.endsWith("#_") ? code.substring(0, code.length() - 2) : code;

        return execute(() -> {
            // Not retried: authorization codes are single-use
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("client_id", properties.clientId());
            form.add("client_secret", properties.clientSecret());
            form.add("grant_type", "authorization_code");
            form.add("redirect_uri", properties.redirectUri());
            form.add("code", cleanCode);
            String shortLivedToken = InstagramResponseParser.parseShortLivedToken(
                    apiClient.postForm("authorization code exchange", properties.tokenUrl(), form));

            SocialToken longLived = retryPolicy.execute(() -> InstagramResponseParser.parseLongLivedToken(
                    apiClient.getUnversioned("long-lived token exchange", "access_token", Map.of(
                            "grant_type", "ig_exchange_token",
                            "client_secret", properties.clientSecret(),
                            "access_token", shortLivedToken)),
                    now()));

            log.info("Exchanged Instagram authorization code for a long-lived token (expires {})", longLived.expiresAt());
            return longLived;
        });
    }

    @Override
    public SocialToken refreshToken(String accessToken) throws SocialConnectorException {
        if (isBlank(accessToken)) {
            throw new SocialConnectorException(
                    SocialConnectorErrorCode.INVALID_CREDENTIALS,
                    "Access token is required for refresh"
            );
        }

        return execute(() -> retryPolicy.execute(() -> InstagramResponseParser.parseLongLivedToken(
                apiClient.getUnversioned("token refresh", "refresh_access_token", Map.of(
                        "grant_type", "ig_refresh_token",
                        "access_token", accessToken)),
                now())));
    }

    private void requireOAuthConfigured() {
        if (!properties.isOAuthConfigured()) {
            throw new SocialConnectorException(
                    SocialConnectorErrorCode.MISSING_CONFIGURATION,
                    "Set social-connector.instagram.client-id, client-secret and redirect-uri to use Instagram OAuth"
            );
        }
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
