package com.media.intelligence.social_connector.connector;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.media.intelligence.social_connector.dto.SocialAuthRequest;
import com.media.intelligence.social_connector.dto.SocialAuthResponse;
import com.media.intelligence.social_connector.dto.SocialComment;
import com.media.intelligence.social_connector.dto.SocialPost;
import com.media.intelligence.social_connector.exception.SocialConnectorErrorCode;
import com.media.intelligence.social_connector.exception.SocialConnectorException;
import com.media.intelligence.social_connector.provider.SocialPlatform;
import com.media.intelligence.social_connector.resilience.RateLimiter;
import com.media.intelligence.social_connector.resilience.RetryPolicy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Instagram Graph API adapter implementation.
 * <p>
 * **Adapter Pattern**: Adapts Instagram Graph API to our SocialMediaProvider interface.
 * Translates Instagram-specific data formats and API calls into our unified domain model.
 * <p>
 * **Anti-Corruption Layer**: Shields our application from Instagram API changes.
 * If Instagram changes their API, we only need to update this adapter.
 * <p>
 * **Instagram Graph API Documentation**:
 * https://developers.facebook.com/docs/instagram-basic-display-api/
 * <p>
 * **Rate Limits**:
 * - 200 calls per hour
 * - User token rate limit
 * <p>
 * **Authentication**:
 * - OAuth 2.0 with Access Token
 * - Long-lived tokens (60 days)
 */
@Slf4j
public class InstagramConnector extends AbstractSocialConnector {

    private static final String BASE_URL = "https://graph.instagram.com";
    private static final int MAX_FETCH_LIMIT = 100; // Instagram API limit
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    /**
     * Constructor with default configuration.
     */
    public InstagramConnector() {
        super();
        this.httpClient = createHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Constructor with custom resilience configuration.
     */
    public InstagramConnector(RateLimiter rateLimiter, RetryPolicy retryPolicy) {
        super(rateLimiter, retryPolicy);
        this.httpClient = createHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Create configured HTTP client.
     */
    private HttpClient createHttpClient() {
        return HttpClient.newBuilder()
                .connectTimeout(REQUEST_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @Override
    public SocialPlatform getPlatform() {
        return SocialPlatform.INSTAGRAM;
    }

    @Override
    protected int getMaxFetchLimit() {
        return MAX_FETCH_LIMIT;
    }

    // ==================== AUTHENTICATION (Step 0) ====================

    @Override
    protected SocialAuthResponse authenticateWithPlatform(SocialAuthRequest authRequest)
            throws SocialConnectorException {
        log.info("Authenticating with Instagram Graph API");

        try {
            // Instagram uses OAuth 2.0 with access tokens
            String accessToken = authRequest.getAccessToken();
            if (accessToken == null || accessToken.trim().isEmpty()) {
                throw new SocialConnectorException(
                        SocialConnectorErrorCode.INVALID_CREDENTIALS,
                        "Instagram requires access token for authentication"
                );
            }

            // Verify token by fetching user profile
            String url = String.format("%s/me?fields=id,username,account_type&access_token=%s",
                    BASE_URL,
                    URLEncoder.encode(accessToken, StandardCharsets.UTF_8));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .timeout(REQUEST_TIMEOUT)
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != HttpStatus.OK.value()) {
                log.error("Instagram authentication failed. Status: {}, Body: {}",
                        response.statusCode(), response.body());
                throw new SocialConnectorException(
                        SocialConnectorErrorCode.AUTHENTICATION_FAILED,
                        "Instagram authentication failed with status: " + response.statusCode()
                );
            }

            // Parse response
            JsonNode jsonResponse = objectMapper.readTree(response.body());

            return SocialAuthResponse.builder()
                    .authenticated(true)
                    .platform(SocialPlatform.INSTAGRAM)
                    .accessToken(accessToken)
                    .userId(jsonResponse.get("id").asText())
                    .username(jsonResponse.get("username").asText())
                    .displayName(jsonResponse.get("username").asText())
                    .tokenExpiresAt(LocalDateTime.now().plusDays(60)) // Instagram tokens: 60 days
                    .rateLimitRemaining(rateLimiter.getRemainingRequests(SocialPlatform.INSTAGRAM))
                    .build();

        } catch (IOException e) {
            throw new SocialConnectorException(
                    SocialConnectorErrorCode.NETWORK_ERROR,
                    "Network error during Instagram authentication",
                    e
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SocialConnectorException(
                    SocialConnectorErrorCode.OPERATION_FAILED,
                    "Authentication interrupted",
                    e
            );
        } catch (Exception e) {
            throw new SocialConnectorException(
                    SocialConnectorErrorCode.AUTHENTICATION_FAILED,
                    "Unexpected error during Instagram authentication",
                    e
            );
        }
    }

    // ==================== API CALLS (Step 3) ====================

    @Override
    protected String callExternalApiForComments(String postId, int limit)
            throws SocialConnectorException {
        log.debug("Calling Instagram API: fetch comments for post {}", postId);

        try {
            // Instagram Graph API: GET /{media-id}/comments
            String url = String.format(
                    "%s/%s/comments?fields=id,text,username,timestamp,like_count&limit=%d&access_token=%s",
                    BASE_URL,
                    URLEncoder.encode(postId, StandardCharsets.UTF_8),
                    Math.min(limit, MAX_FETCH_LIMIT),
                    URLEncoder.encode(authResponse.getAccessToken(), StandardCharsets.UTF_8)
            );

            return executeHttpGet(url);

        } catch (Exception e) {
            throw new SocialConnectorException(
                    SocialConnectorErrorCode.API_CALL_FAILED,
                    "Failed to fetch comments from Instagram",
                    e
            );
        }
    }

    @Override
    protected String callExternalApiForPosts(String userId, int limit)
            throws SocialConnectorException {
        log.debug("Calling Instagram API: fetch posts for user {}", userId);

        try {
            // Instagram Graph API: GET /{user-id}/media
            String url = String.format(
                    "%s/%s/media?fields=id,caption,media_type,media_url,permalink,timestamp,like_count,comments_count&limit=%d&access_token=%s",
                    BASE_URL,
                    URLEncoder.encode(userId, StandardCharsets.UTF_8),
                    Math.min(limit, MAX_FETCH_LIMIT),
                    URLEncoder.encode(authResponse.getAccessToken(), StandardCharsets.UTF_8)
            );

            return executeHttpGet(url);

        } catch (Exception e) {
            throw new SocialConnectorException(
                    SocialConnectorErrorCode.API_CALL_FAILED,
                    "Failed to fetch posts from Instagram",
                    e
            );
        }
    }

    @Override
    protected String callExternalApiForPost(String postId)
            throws SocialConnectorException {
        log.debug("Calling Instagram API: fetch post {}", postId);

        try {
            // Instagram Graph API: GET /{media-id}
            String url = String.format(
                    "%s/%s?fields=id,caption,media_type,media_url,permalink,timestamp,like_count,comments_count,owner&access_token=%s",
                    BASE_URL,
                    URLEncoder.encode(postId, StandardCharsets.UTF_8),
                    URLEncoder.encode(authResponse.getAccessToken(), StandardCharsets.UTF_8)
            );

            return executeHttpGet(url);

        } catch (Exception e) {
            throw new SocialConnectorException(
                    SocialConnectorErrorCode.API_CALL_FAILED,
                    "Failed to fetch post from Instagram",
                    e
            );
        }
    }

    /**
     * Execute HTTP GET request.
     */
    private String executeHttpGet(String url) throws IOException, InterruptedException, SocialConnectorException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "application/json")
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        // Handle different status codes
        if (response.statusCode() == 200) {
            return response.body();
        } else if (response.statusCode() == 401) {
            throw new SocialConnectorException(
                    SocialConnectorErrorCode.AUTHENTICATION_FAILED,
                    "Instagram authentication failed (401)"
            );
        } else if (response.statusCode() == 429) {
            throw new SocialConnectorException(
                    SocialConnectorErrorCode.RATE_LIMIT_EXCEEDED,
                    "Instagram rate limit exceeded (429)"
            );
        } else if (response.statusCode() == 404) {
            throw new SocialConnectorException(
                    SocialConnectorErrorCode.RESOURCE_NOT_FOUND,
                    "Instagram resource not found (404)"
            );
        } else if (response.statusCode() >= 500) {
            throw new SocialConnectorException(
                    SocialConnectorErrorCode.PLATFORM_UNAVAILABLE,
                    "Instagram service unavailable (" + response.statusCode() + ")"
            );
        } else {
            throw new SocialConnectorException(
                    SocialConnectorErrorCode.API_CALL_FAILED,
                    "Instagram API call failed with status: " + response.statusCode()
            );
        }
    }

    // ==================== RESPONSE PARSING (Step 4) ====================

    @Override
    protected List<SocialComment> parseCommentsResponse(String rawResponse)
            throws SocialConnectorException {
        log.debug("Parsing Instagram comments response");

        try {
            List<SocialComment> comments = new ArrayList<>();
            JsonNode root = objectMapper.readTree(rawResponse);
            JsonNode dataArray = root.get("data");

            if (dataArray == null || !dataArray.isArray()) {
                log.warn("Instagram response has no 'data' array");
                return comments;
            }

            for (JsonNode commentNode : dataArray) {
                SocialComment comment = SocialComment.builder()
                        .commentId(commentNode.get("id").asText())
                        .platform(SocialPlatform.INSTAGRAM)
                        .text(commentNode.get("text").asText())
                        .authorUsername(commentNode.get("username").asText())
                        .authorDisplayName(commentNode.get("username").asText())
                        .likes(commentNode.has("like_count") ? commentNode.get("like_count").asInt() : 0)
                        .createdAt(parseInstagramTimestamp(commentNode.get("timestamp").asText()))
                        .sanitized(false) // Will be sanitized by template method
                        .build();

                comments.add(comment);
            }

            log.debug("Parsed {} comments from Instagram response", comments.size());
            return comments;

        } catch (Exception e) {
            throw new SocialConnectorException(
                    SocialConnectorErrorCode.PARSE_ERROR,
                    "Failed to parse Instagram comments response",
                    e
            );
        }
    }

    @Override
    protected List<SocialPost> parsePostsResponse(String rawResponse)
            throws SocialConnectorException {
        log.debug("Parsing Instagram posts response");

        try {
            List<SocialPost> posts = new ArrayList<>();
            JsonNode root = objectMapper.readTree(rawResponse);
            JsonNode dataArray = root.get("data");

            if (dataArray == null || !dataArray.isArray()) {
                log.warn("Instagram response has no 'data' array");
                return posts;
            }

            for (JsonNode postNode : dataArray) {
                SocialPost post = parsePostNode(postNode);
                posts.add(post);
            }

            log.debug("Parsed {} posts from Instagram response", posts.size());
            return posts;

        } catch (Exception e) {
            throw new SocialConnectorException(
                    SocialConnectorErrorCode.PARSE_ERROR,
                    "Failed to parse Instagram posts response",
                    e
            );
        }
    }

    @Override
    protected SocialPost parsePostResponse(String rawResponse)
            throws SocialConnectorException {
        log.debug("Parsing Instagram post response");

        try {
            JsonNode postNode = objectMapper.readTree(rawResponse);
            return parsePostNode(postNode);

        } catch (Exception e) {
            throw new SocialConnectorException(
                    SocialConnectorErrorCode.PARSE_ERROR,
                    "Failed to parse Instagram post response",
                    e
            );
        }
    }

    /**
     * Parse Instagram post JSON node into SocialPost.
     */
    private SocialPost parsePostNode(JsonNode postNode) {
        List<String> mediaUrls = new ArrayList<>();
        if (postNode.has("media_url")) {
            mediaUrls.add(postNode.get("media_url").asText());
        }

        return SocialPost.builder()
                .postId(postNode.get("id").asText())
                .platform(SocialPlatform.INSTAGRAM)
                .caption(postNode.has("caption") ? postNode.get("caption").asText() : "")
                .postType(postNode.has("media_type") ? postNode.get("media_type").asText() : "UNKNOWN")
                .mediaUrls(mediaUrls)
                .likes(postNode.has("like_count") ? postNode.get("like_count").asInt() : 0)
                .commentCount(postNode.has("comments_count") ? postNode.get("comments_count").asInt() : 0)
                .createdAt(postNode.has("timestamp") ? parseInstagramTimestamp(postNode.get("timestamp").asText()) : null)
                .sanitized(false) // Will be sanitized by template method
                .build();
    }

    /**
     * Parse Instagram timestamp format to LocalDateTime.
     * Instagram format: "2023-12-31T10:30:00+0000"
     */
    private LocalDateTime parseInstagramTimestamp(String timestamp) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
            return LocalDateTime.parse(timestamp.replaceAll("\\+0000$", ""), formatter);
        } catch (Exception e) {
            log.warn("Failed to parse Instagram timestamp: {}", timestamp);
            return LocalDateTime.now();
        }
    }

    @Override
    protected void performDisconnect() {
        // Instagram doesn't require token revocation for Basic Display API
        // For production, consider revoking long-lived tokens
        log.debug("Instagram connector disconnected");
    }
}
