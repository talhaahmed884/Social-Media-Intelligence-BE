package com.media.intelligence.social_connector.connector.instagram;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.media.intelligence.social_connector.dto.SocialAuthRequest;
import com.media.intelligence.social_connector.dto.SocialAuthResponse;
import com.media.intelligence.social_connector.dto.SocialComment;
import com.media.intelligence.social_connector.dto.SocialCredentials;
import com.media.intelligence.social_connector.dto.SocialPost;
import com.media.intelligence.social_connector.dto.SocialToken;
import com.media.intelligence.social_connector.exception.SocialConnectorErrorCode;
import com.media.intelligence.social_connector.exception.SocialConnectorException;
import com.media.intelligence.social_connector.provider.SocialPlatform;
import com.media.intelligence.social_connector.resilience.RateLimiter;
import com.media.intelligence.social_connector.resilience.RetryPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

@DisplayName("InstagramConnector Tests")
public class InstagramConnectorTest {

    private static final String GRAPH = "https://graph.instagram.com";
    private static final String GRAPH_V = GRAPH + "/v26.0";
    private static final String TOKEN_URL = "https://api.instagram.com/oauth/access_token";
    private static final String ACCOUNT_ID = "17841400000000000";
    private static final String TOKEN = "test-token";
    private static final int MAX_ATTEMPTS = 3;
    private static final int INSTAGRAM_LIMIT = 200;
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private MockRestServiceServer server;
    private RateLimiter rateLimiter;
    private InstagramConnector connector;
    private SocialCredentials credentials;

    @BeforeEach
    void setUp() {
        connector = newConnector(properties("client-1", "secret-1", "https://app.example/callback"));
        credentials = new SocialCredentials(SocialPlatform.INSTAGRAM, ACCOUNT_ID, TOKEN, null);
    }

    // ==================== fetchComments Tests ====================

    @Test
    @DisplayName("fetchComments: Should call the versioned endpoint, then parse, sanitize, and set postId")
    void fetchComments_ShouldParseAndSanitize() {
        // Arrange
        server.expect(requestTo(startsWith(GRAPH_V + "/post-1/comments?")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("access_token", TOKEN))
                .andExpect(queryParam("limit", "10"))
                .andRespond(withSuccess("""
                        {"data": [
                          {"id": "c1", "text": "<script>alert(1)</script>Nice <b>post</b>", "username": " alice ",
                           "timestamp": "2024-01-15T10:30:00+0000", "like_count": 5},
                          {"id": "c2", "text": "Fish & chips", "username": "bob", "timestamp": "2024-01-15T11:00:00+0000"}
                        ]}
                        """, MediaType.APPLICATION_JSON));

        // Act
        List<SocialComment> comments = connector.fetchComments(credentials, "post-1", 10);

        // Assert
        server.verify();
        assertEquals(2, comments.size());

        SocialComment first = comments.get(0);
        assertEquals("c1", first.getCommentId());
        assertEquals("post-1", first.getPostId());
        assertEquals("Nice post", first.getText());
        assertEquals("alice", first.getAuthorUsername());
        assertEquals(5, first.getLikes());
        assertEquals(LocalDateTime.of(2024, 1, 15, 10, 30), first.getCreatedAt());
        assertTrue(first.isSanitized());

        assertEquals("Fish & chips", comments.get(1).getText(), "Plain text must not be HTML-escaped");
        assertEquals(INSTAGRAM_LIMIT - 1, connector.getRateLimitRemaining(ACCOUNT_ID));
    }

    @Test
    @DisplayName("fetchComments: Should map Graph error 190 to AUTHENTICATION_FAILED without retrying")
    void fetchComments_ShouldMapInvalidTokenWithoutRetry() {
        // Arrange
        server.expect(ExpectedCount.once(), requestTo(startsWith(GRAPH_V)))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\": {\"message\": \"Invalid OAuth access token\", \"code\": 190}}"));

        // Act & Assert
        assertErrorCode(SocialConnectorErrorCode.AUTHENTICATION_FAILED,
                () -> connector.fetchComments(credentials, "post-1", 10));
        server.verify();
    }

    @Test
    @DisplayName("fetchComments: Should map Graph error 190/463 to TOKEN_EXPIRED")
    void fetchComments_ShouldMapExpiredToken() {
        // Arrange
        server.expect(requestTo(startsWith(GRAPH_V)))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\": {\"message\": \"Session has expired\", \"code\": 190, \"error_subcode\": 463}}"));

        // Act & Assert
        assertErrorCode(SocialConnectorErrorCode.TOKEN_EXPIRED,
                () -> connector.fetchComments(credentials, "post-1", 10));
    }

    @Test
    @DisplayName("fetchComments: Should map Graph rate limit codes to RATE_LIMIT_EXCEEDED without retrying")
    void fetchComments_ShouldMapPlatformRateLimit() {
        // Arrange
        server.expect(ExpectedCount.once(), requestTo(startsWith(GRAPH_V)))
                .andRespond(withStatus(HttpStatus.FORBIDDEN).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\": {\"message\": \"Application request limit reached\", \"code\": 4}}"));

        // Act & Assert
        assertErrorCode(SocialConnectorErrorCode.RATE_LIMIT_EXCEEDED,
                () -> connector.fetchComments(credentials, "post-1", 10));
        server.verify();
    }

    @Test
    @DisplayName("fetchComments: Should retry 5xx and count every attempt against the rate limit")
    void fetchComments_ShouldRetryServerErrors() {
        // Arrange
        server.expect(ExpectedCount.times(MAX_ATTEMPTS), requestTo(startsWith(GRAPH_V)))
                .andRespond(withServerError());

        // Act & Assert
        assertErrorCode(SocialConnectorErrorCode.PLATFORM_UNAVAILABLE,
                () -> connector.fetchComments(credentials, "post-1", 10));
        server.verify();
        assertEquals(INSTAGRAM_LIMIT - MAX_ATTEMPTS, connector.getRateLimitRemaining(ACCOUNT_ID));
    }

    @Test
    @DisplayName("fetchComments: Should retry network errors and succeed")
    void fetchComments_ShouldRetryNetworkErrors() {
        // Arrange
        server.expect(requestTo(startsWith(GRAPH_V))).andRespond(withException(new IOException("connection reset")));
        server.expect(requestTo(startsWith(GRAPH_V))).andRespond(withSuccess("{\"data\": []}", MediaType.APPLICATION_JSON));

        // Act
        List<SocialComment> comments = connector.fetchComments(credentials, "post-1", 10);

        // Assert
        assertTrue(comments.isEmpty());
        server.verify();
    }

    @Test
    @DisplayName("fetchComments: Should report NETWORK_ERROR without leaking the token in the exception chain")
    void fetchComments_ShouldNotLeakTokenOnNetworkError() {
        // Arrange
        server.expect(ExpectedCount.times(MAX_ATTEMPTS), requestTo(startsWith(GRAPH_V)))
                .andRespond(withException(new IOException("connection reset")));

        // Act
        SocialConnectorException ex = assertThrows(SocialConnectorException.class, () ->
                connector.fetchComments(credentials, "post-1", 10));

        // Assert
        assertEquals(SocialConnectorErrorCode.NETWORK_ERROR, ex.getErrorCode());
        for (Throwable t = ex; t != null; t = t.getCause()) {
            assertFalse(String.valueOf(t.getMessage()).contains(TOKEN), "Token leaked in: " + t);
        }
    }

    @Test
    @DisplayName("fetchComments: Should map malformed JSON to PARSE_ERROR")
    void fetchComments_ShouldMapMalformedJson() {
        // Arrange
        server.expect(requestTo(startsWith(GRAPH_V))).andRespond(withSuccess("not json", MediaType.APPLICATION_JSON));

        // Act & Assert
        assertErrorCode(SocialConnectorErrorCode.PARSE_ERROR,
                () -> connector.fetchComments(credentials, "post-1", 10));
    }

    @Test
    @DisplayName("fetchComments: Should validate credentials and limits without calling the API")
    void fetchComments_ShouldValidateBeforeCalling() {
        // Arrange
        SocialCredentials expired = new SocialCredentials(
                SocialPlatform.INSTAGRAM, ACCOUNT_ID, TOKEN, LocalDateTime.now(ZoneOffset.UTC).minusMinutes(1));
        SocialCredentials twitter = new SocialCredentials(SocialPlatform.TWITTER, ACCOUNT_ID, TOKEN, null);

        // Act & Assert
        assertErrorCode(SocialConnectorErrorCode.TOKEN_EXPIRED, () -> connector.fetchComments(expired, "post-1", 10));
        assertErrorCode(SocialConnectorErrorCode.CONFIGURATION_ERROR, () -> connector.fetchComments(twitter, "post-1", 10));
        assertErrorCode(SocialConnectorErrorCode.DATA_VALIDATION_FAILED, () -> connector.fetchComments(credentials, "post-1", 51));
        assertErrorCode(SocialConnectorErrorCode.DATA_VALIDATION_FAILED, () -> connector.fetchComments(credentials, " ", 10));
        server.verify();
    }

    @Test
    @DisplayName("fetchComments: Should stop before calling the API when local rate limit is exhausted")
    void fetchComments_ShouldStopWhenLocalRateLimitExhausted() {
        // Arrange
        for (int i = 0; i < INSTAGRAM_LIMIT; i++) {
            rateLimiter.checkRateLimit(SocialPlatform.INSTAGRAM, ACCOUNT_ID);
        }

        // Act & Assert
        assertErrorCode(SocialConnectorErrorCode.RATE_LIMIT_EXCEEDED,
                () -> connector.fetchComments(credentials, "post-1", 10));
        server.verify();
    }

    // ==================== fetchPost / fetchPosts Tests ====================

    @Test
    @DisplayName("fetchPost: Should parse and sanitize a single post")
    void fetchPost_ShouldParseAndSanitize() {
        // Arrange
        server.expect(requestTo(startsWith(GRAPH_V + "/m1?")))
                .andRespond(withSuccess("""
                        {"id": "m1", "caption": "  Hello <i>world</i>  ", "media_type": "IMAGE",
                         "media_url": "https://cdn.example/m1.jpg", "like_count": 12, "comments_count": 3,
                         "timestamp": "2024-02-01T08:00:00+0000", "owner": {"id": "owner-1"}, "username": "alice"}
                        """, MediaType.APPLICATION_JSON));

        // Act
        SocialPost post = connector.fetchPost(credentials, "m1");

        // Assert
        assertEquals("m1", post.getPostId());
        assertEquals("Hello world", post.getCaption());
        assertEquals("IMAGE", post.getPostType());
        assertEquals(List.of("https://cdn.example/m1.jpg"), post.getMediaUrls());
        assertEquals("owner-1", post.getAuthorId());
        assertEquals("alice", post.getAuthorUsername());
        assertTrue(post.isSanitized());
    }

    @Test
    @DisplayName("fetchPosts: Should call the account media endpoint")
    void fetchPosts_ShouldCallMediaEndpoint() {
        // Arrange
        server.expect(requestTo(startsWith(GRAPH_V + "/" + ACCOUNT_ID + "/media?")))
                .andExpect(queryParam("limit", "5"))
                .andRespond(withSuccess("{\"data\": [{\"id\": \"m1\"}, {\"id\": \"m2\"}]}", MediaType.APPLICATION_JSON));

        // Act
        List<SocialPost> posts = connector.fetchPosts(credentials, ACCOUNT_ID, 5);

        // Assert
        assertEquals(List.of("m1", "m2"), posts.stream().map(SocialPost::getPostId).toList());
    }

    // ==================== connect Tests ====================

    @Test
    @DisplayName("connect: Should return account info usable as credentials")
    void connect_ShouldReturnAccountInfo() {
        // Arrange
        server.expect(requestTo(startsWith(GRAPH_V + "/me?")))
                .andExpect(queryParam("access_token", TOKEN))
                .andRespond(withSuccess(
                        "{\"user_id\": \"" + ACCOUNT_ID + "\", \"username\": \"alice\", \"account_type\": \"BUSINESS\"}",
                        MediaType.APPLICATION_JSON));

        // Act
        SocialAuthResponse response = connector.connect(authRequest(TOKEN));

        // Assert
        assertTrue(response.isAuthenticated());
        assertEquals(ACCOUNT_ID, response.getUserId());
        assertEquals("alice", response.getUsername());
        assertEquals(credentials, SocialCredentials.from(response));
        assertFalse(response.toString().contains(TOKEN), "Access token must not appear in toString");
    }

    @Test
    @DisplayName("connect: Should fail when profile response has no user_id, or token is missing")
    void connect_ShouldValidateProfileAndToken() {
        // Arrange
        server.expect(requestTo(startsWith(GRAPH_V + "/me?")))
                .andRespond(withSuccess("{\"username\": \"alice\"}", MediaType.APPLICATION_JSON));

        // Act & Assert
        assertErrorCode(SocialConnectorErrorCode.API_RESPONSE_INVALID, () -> connector.connect(authRequest(TOKEN)));
        assertErrorCode(SocialConnectorErrorCode.INVALID_CREDENTIALS, () -> connector.connect(authRequest(null)));
        server.verify();
    }

    // ==================== OAuth Tests ====================

    @Test
    @DisplayName("buildAuthorizationUrl: Should include client, redirect, scopes, and state")
    void buildAuthorizationUrl_ShouldIncludeParameters() {
        // Act
        String url = connector.buildAuthorizationUrl("state-123");

        // Assert
        assertTrue(url.startsWith("https://www.instagram.com/oauth/authorize?"), url);
        Map<String, String> params = decodedQuery(url);
        assertEquals("client-1", params.get("client_id"));
        assertEquals("https://app.example/callback", params.get("redirect_uri"));
        assertEquals("code", params.get("response_type"));
        assertEquals("instagram_business_basic,instagram_business_manage_comments", params.get("scope"));
        assertEquals("state-123", params.get("state"));
    }

    @Test
    @DisplayName("exchangeAuthorizationCode: Should exchange code for short-lived, then long-lived token")
    void exchangeAuthorizationCode_ShouldReturnLongLivedToken() {
        // Arrange
        server.expect(requestTo(TOKEN_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().formDataContains(Map.of(
                        "client_id", "client-1",
                        "client_secret", "secret-1",
                        "grant_type", "authorization_code",
                        "redirect_uri", "https://app.example/callback",
                        "code", "abc")))
                .andRespond(withSuccess(
                        "{\"data\": [{\"access_token\": \"short\", \"user_id\": \"1\", \"permissions\": \"instagram_business_basic\"}]}",
                        MediaType.APPLICATION_JSON));
        server.expect(requestTo(startsWith(GRAPH + "/access_token?")))
                .andExpect(queryParam("grant_type", "ig_exchange_token"))
                .andExpect(queryParam("client_secret", "secret-1"))
                .andExpect(queryParam("access_token", "short"))
                .andRespond(withSuccess(
                        "{\"access_token\": \"long\", \"token_type\": \"bearer\", \"expires_in\": 5184000}",
                        MediaType.APPLICATION_JSON));

        // Act: Instagram appends "#_" to the code
        SocialToken token = connector.exchangeAuthorizationCode("abc#_");

        // Assert
        server.verify();
        assertEquals("long", token.accessToken());
        assertEquals(LocalDateTime.ofInstant(NOW, ZoneOffset.UTC).plusSeconds(5184000), token.expiresAt());
    }

    @Test
    @DisplayName("exchangeAuthorizationCode: Should map a used/invalid code to INVALID_CREDENTIALS without retrying")
    void exchangeAuthorizationCode_ShouldMapInvalidCode() {
        // Arrange
        server.expect(ExpectedCount.once(), requestTo(TOKEN_URL))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON).body(
                        "{\"error_type\": \"OAuthException\", \"code\": 400, \"error_message\": \"Matching code was not found or was already used\"}"));

        // Act & Assert
        assertErrorCode(SocialConnectorErrorCode.INVALID_CREDENTIALS, () -> connector.exchangeAuthorizationCode("used"));
        server.verify();
    }

    @Test
    @DisplayName("OAuth: Should throw MISSING_CONFIGURATION when client credentials are not set")
    void oauth_ShouldRequireConfiguration() {
        // Arrange
        InstagramConnector unconfigured = newConnector(properties(null, null, null));

        // Act & Assert
        assertErrorCode(SocialConnectorErrorCode.MISSING_CONFIGURATION, () -> unconfigured.buildAuthorizationUrl("s"));
        assertErrorCode(SocialConnectorErrorCode.MISSING_CONFIGURATION, () -> unconfigured.exchangeAuthorizationCode("c"));
    }

    @Test
    @DisplayName("refreshToken: Should call refresh endpoint and compute new expiry")
    void refreshToken_ShouldReturnRefreshedToken() {
        // Arrange
        server.expect(requestTo(startsWith(GRAPH + "/refresh_access_token?")))
                .andExpect(queryParam("grant_type", "ig_refresh_token"))
                .andExpect(queryParam("access_token", TOKEN))
                .andRespond(withSuccess(
                        "{\"access_token\": \"refreshed\", \"token_type\": \"bearer\", \"expires_in\": 3600}",
                        MediaType.APPLICATION_JSON));

        // Act
        SocialToken token = connector.refreshToken(TOKEN);

        // Assert
        assertEquals("refreshed", token.accessToken());
        assertEquals(LocalDateTime.ofInstant(NOW, ZoneOffset.UTC).plusHours(1), token.expiresAt());
        assertFalse(token.toString().contains("refreshed"), "Token must not appear in toString");
    }

    @Test
    @DisplayName("refreshToken: Should map invalid token to AUTHENTICATION_FAILED")
    void refreshToken_ShouldMapInvalidToken() {
        // Arrange
        server.expect(requestTo(startsWith(GRAPH + "/refresh_access_token?")))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\": {\"message\": \"Invalid OAuth access token\", \"code\": 190}}"));

        // Act & Assert
        assertErrorCode(SocialConnectorErrorCode.AUTHENTICATION_FAILED, () -> connector.refreshToken(TOKEN));
    }

    // ==================== Helpers ====================

    private InstagramConnector newConnector(InstagramProperties properties) {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        rateLimiter = new RateLimiter();
        RetryPolicy retryPolicy = RetryPolicy.builder().maxAttempts(MAX_ATTEMPTS).initialDelayMs(0).build();
        InstagramApiClient apiClient = new InstagramApiClient(builder.build(), properties, new ObjectMapper());
        return new InstagramConnector(rateLimiter, retryPolicy, apiClient, properties, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static InstagramProperties properties(String clientId, String clientSecret, String redirectUri) {
        return new InstagramProperties(GRAPH, "v26.0", "https://www.instagram.com/oauth/authorize", TOKEN_URL,
                clientId, clientSecret, redirectUri,
                List.of("instagram_business_basic", "instagram_business_manage_comments"), 50);
    }

    private static SocialAuthRequest authRequest(String token) {
        return SocialAuthRequest.builder().platform(SocialPlatform.INSTAGRAM).accessToken(token).build();
    }

    private static void assertErrorCode(SocialConnectorErrorCode expected, Executable executable) {
        SocialConnectorException ex = assertThrows(SocialConnectorException.class, executable);
        assertEquals(expected, ex.getErrorCode());
    }

    private static Map<String, String> decodedQuery(String url) {
        return UriComponentsBuilder.fromUriString(url).build().getQueryParams().toSingleValueMap().entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        e -> URLDecoder.decode(e.getValue(), StandardCharsets.UTF_8)));
    }
}
