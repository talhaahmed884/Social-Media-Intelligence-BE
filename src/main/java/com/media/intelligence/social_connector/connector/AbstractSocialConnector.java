package com.media.intelligence.social_connector.connector;

import com.media.intelligence.social_connector.dto.SocialAuthRequest;
import com.media.intelligence.social_connector.dto.SocialAuthResponse;
import com.media.intelligence.social_connector.dto.SocialComment;
import com.media.intelligence.social_connector.dto.SocialPost;
import com.media.intelligence.social_connector.exception.SocialConnectorErrorCode;
import com.media.intelligence.social_connector.exception.SocialConnectorException;
import com.media.intelligence.social_connector.provider.SocialMediaProvider;
import com.media.intelligence.social_connector.resilience.RateLimiter;
import com.media.intelligence.social_connector.resilience.RetryPolicy;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringEscapeUtils;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Abstract base class for all social media connectors.
 * <p>
 * **Template Method Pattern**: Defines the skeleton of the data fetching algorithm.
 * Subclasses override specific steps while the overall flow remains fixed.
 * <p>
 * **Fixed Algorithm Flow**:
 * 1. validateAuth() - Verify authentication
 * 2. checkRateLimit() - Apply rate limiting
 * 3. callExternalApi() - Call platform API (abstract - subclass implements)
 * 4. parseResponse() - Parse API response (abstract - subclass implements)
 * 5. sanitizeData() - Clean and validate data
 * <p>
 * **Anti-Corruption Layer**: This class shields our domain from external API specifics.
 * Platform-specific logic is delegated to concrete adapter implementations.
 */
@Slf4j
public abstract class AbstractSocialConnector implements SocialMediaProvider {

    // XSS protection patterns
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]+>");
    private static final Pattern SCRIPT_PATTERN = Pattern.compile("<script[^>]*>.*?</script>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    // Resilience components
    protected final RateLimiter rateLimiter;
    protected final RetryPolicy retryPolicy;
    // Authentication state
    protected SocialAuthResponse authResponse;
    protected boolean connected = false;

    /**
     * Constructor with default resilience configuration.
     */
    protected AbstractSocialConnector() {
        this.rateLimiter = new RateLimiter();
        this.retryPolicy = RetryPolicy.defaultPolicy();
    }

    /**
     * Constructor with custom resilience configuration.
     *
     * @param rateLimiter custom rate limiter
     * @param retryPolicy custom retry policy
     */
    protected AbstractSocialConnector(RateLimiter rateLimiter, RetryPolicy retryPolicy) {
        this.rateLimiter = rateLimiter;
        this.retryPolicy = retryPolicy;
    }

    // ==================== PUBLIC API METHODS ====================

    @Override
    public SocialAuthResponse connect(SocialAuthRequest authRequest) throws SocialConnectorException {
        log.info("Connecting to {} platform", getPlatform().getDisplayName());

        try {
            // Validate request
            validateAuthRequest(authRequest);

            // Authenticate with platform (delegate to subclass)
            this.authResponse = retryPolicy.execute(() -> authenticateWithPlatform(authRequest));
            this.connected = authResponse.isAuthenticated();

            if (!connected) {
                throw new SocialConnectorException(
                        SocialConnectorErrorCode.AUTHENTICATION_FAILED,
                        "Authentication failed for " + getPlatform().getDisplayName()
                );
            }

            log.info("Successfully connected to {} as user: {}",
                    getPlatform().getDisplayName(),
                    authResponse.getUsername());

            return authResponse;

        } catch (SocialConnectorException e) {
            log.error("Failed to connect to {}: {}", getPlatform().getDisplayName(), e.getMessage());
            this.connected = false;
            throw e;
        }
    }

    @Override
    public List<SocialComment> fetchComments(String postId, int limit) throws SocialConnectorException {
        log.debug("Fetching {} comments for post: {} from {}",
                limit, postId, getPlatform().getDisplayName());

        // **Template Method**: Execute fixed algorithm
        return executeFetchTemplate(() -> {
            // Step 1: Validate (implemented here)
            validateAuth();
            validatePostId(postId);
            validateLimit(limit);

            // Step 2: Rate limiting (implemented here)
            rateLimiter.checkRateLimit(getPlatform());

            // Step 3: Call external API (delegated to subclass)
            String rawResponse = retryPolicy.execute(() ->
                    callExternalApiForComments(postId, limit)
            );

            // Step 4: Parse response (delegated to subclass)
            List<SocialComment> comments = parseCommentsResponse(rawResponse);

            // Step 5: Sanitize data (implemented here)
            sanitizeComments(comments);

            log.info("Successfully fetched and sanitized {} comments from {}",
                    comments.size(), getPlatform().getDisplayName());

            return comments;
        });
    }

    @Override
    public List<SocialPost> fetchPosts(String userId, int limit) throws SocialConnectorException {
        log.debug("Fetching {} posts for user: {} from {}",
                limit, userId, getPlatform().getDisplayName());

        return executeFetchTemplate(() -> {
            validateAuth();
            validateUserId(userId);
            validateLimit(limit);

            rateLimiter.checkRateLimit(getPlatform());

            String rawResponse = retryPolicy.execute(() ->
                    callExternalApiForPosts(userId, limit)
            );

            List<SocialPost> posts = parsePostsResponse(rawResponse);
            sanitizePosts(posts);

            log.info("Successfully fetched and sanitized {} posts from {}",
                    posts.size(), getPlatform().getDisplayName());

            return posts;
        });
    }

    @Override
    public SocialPost fetchPost(String postId) throws SocialConnectorException {
        log.debug("Fetching post: {} from {}", postId, getPlatform().getDisplayName());

        return executeFetchTemplate(() -> {
            validateAuth();
            validatePostId(postId);

            rateLimiter.checkRateLimit(getPlatform());

            String rawResponse = retryPolicy.execute(() ->
                    callExternalApiForPost(postId)
            );

            SocialPost post = parsePostResponse(rawResponse);
            sanitizePost(post);

            log.info("Successfully fetched and sanitized post {} from {}",
                    postId, getPlatform().getDisplayName());

            return post;
        });
    }

    @Override
    public boolean isConnected() {
        return connected && authResponse != null && !isTokenExpired();
    }

    @Override
    public void disconnect() {
        log.info("Disconnecting from {}", getPlatform().getDisplayName());
        this.connected = false;
        this.authResponse = null;
        // Subclasses can override to revoke tokens
        performDisconnect();
    }

    @Override
    public int getRateLimitRemaining() {
        return rateLimiter.getRemainingRequests(getPlatform());
    }

    // ==================== TEMPLATE METHOD EXECUTION ====================

    /**
     * Template method executor with exception handling.
     * <p>
     * Wraps the execution in try-catch to translate exceptions
     * into our domain exception type.
     *
     * @param operation operation to execute
     * @param <T>       return type
     * @return operation result
     */
    private <T> T executeFetchTemplate(FetchOperation<T> operation) {
        try {
            return operation.execute();
        } catch (SocialConnectorException e) {
            // Already our exception type
            throw e;
        } catch (Exception e) {
            // Translate unexpected exceptions
            log.error("Unexpected error during fetch operation: {}", e.getMessage(), e);
            throw new SocialConnectorException(
                    SocialConnectorErrorCode.OPERATION_FAILED,
                    "Unexpected error during data fetch",
                    e
            );
        }
    }

    // ==================== VALIDATION METHODS (Step 1) ====================

    /**
     * Step 1a: Validate authentication state.
     *
     * @throws SocialConnectorException if not authenticated
     */
    protected void validateAuth() throws SocialConnectorException {
        if (!connected || authResponse == null) {
            throw new SocialConnectorException(
                    SocialConnectorErrorCode.AUTHENTICATION_FAILED,
                    "Not connected to " + getPlatform().getDisplayName()
            );
        }

        if (isTokenExpired()) {
            throw new SocialConnectorException(
                    SocialConnectorErrorCode.TOKEN_EXPIRED,
                    "Access token expired for " + getPlatform().getDisplayName()
            );
        }
    }

    /**
     * Step 1b: Validate auth request.
     *
     * @param request auth request
     */
    protected void validateAuthRequest(SocialAuthRequest request) throws SocialConnectorException {
        if (request == null) {
            throw new SocialConnectorException(
                    SocialConnectorErrorCode.CONFIGURATION_ERROR,
                    "Auth request cannot be null"
            );
        }

        if (request.getPlatform() != getPlatform()) {
            throw new SocialConnectorException(
                    SocialConnectorErrorCode.CONFIGURATION_ERROR,
                    "Platform mismatch: expected " + getPlatform() + ", got " + request.getPlatform()
            );
        }

        if (request.getAccessToken() == null && request.getApiKey() == null) {
            throw new SocialConnectorException(
                    SocialConnectorErrorCode.INVALID_CREDENTIALS,
                    "Access token or API key required"
            );
        }
    }

    /**
     * Validate post ID.
     */
    protected void validatePostId(String postId) throws SocialConnectorException {
        if (postId == null || postId.trim().isEmpty()) {
            throw new SocialConnectorException(
                    SocialConnectorErrorCode.DATA_VALIDATION_FAILED,
                    "Post ID cannot be null or empty"
            );
        }
    }

    /**
     * Validate user ID.
     */
    protected void validateUserId(String userId) throws SocialConnectorException {
        if (userId == null || userId.trim().isEmpty()) {
            throw new SocialConnectorException(
                    SocialConnectorErrorCode.DATA_VALIDATION_FAILED,
                    "User ID cannot be null or empty"
            );
        }
    }

    /**
     * Validate limit parameter.
     */
    protected void validateLimit(int limit) throws SocialConnectorException {
        if (limit <= 0) {
            throw new SocialConnectorException(
                    SocialConnectorErrorCode.DATA_VALIDATION_FAILED,
                    "Limit must be greater than 0"
            );
        }

        if (limit > getMaxFetchLimit()) {
            throw new SocialConnectorException(
                    SocialConnectorErrorCode.DATA_VALIDATION_FAILED,
                    "Limit exceeds maximum allowed: " + getMaxFetchLimit()
            );
        }
    }

    /**
     * Check if access token is expired.
     */
    protected boolean isTokenExpired() {
        if (authResponse == null || authResponse.getTokenExpiresAt() == null) {
            return false; // No expiration time set
        }
        return authResponse.getTokenExpiresAt().isBefore(java.time.LocalDateTime.now());
    }

    // ==================== SANITIZATION METHODS (Step 5) ====================

    /**
     * Step 5a: Sanitize list of comments.
     */
    protected void sanitizeComments(List<SocialComment> comments) {
        if (comments == null) return;

        comments.forEach(this::sanitizeComment);
    }

    /**
     * Sanitize single comment.
     */
    protected void sanitizeComment(SocialComment comment) {
        if (comment == null) return;

        // Sanitize text content
        comment.setText(sanitizeText(comment.getText()));

        // Sanitize author fields
        comment.setAuthorUsername(sanitizeText(comment.getAuthorUsername()));
        comment.setAuthorDisplayName(sanitizeText(comment.getAuthorDisplayName()));

        comment.setSanitized(true);
    }

    /**
     * Step 5b: Sanitize list of posts.
     */
    protected void sanitizePosts(List<SocialPost> posts) {
        if (posts == null) return;

        posts.forEach(this::sanitizePost);
    }

    /**
     * Sanitize single post.
     */
    protected void sanitizePost(SocialPost post) {
        if (post == null) return;

        // Sanitize text content
        post.setCaption(sanitizeText(post.getCaption()));

        // Sanitize author fields
        post.setAuthorUsername(sanitizeText(post.getAuthorUsername()));
        post.setAuthorDisplayName(sanitizeText(post.getAuthorDisplayName()));

        // Sanitize hashtags and mentions
        if (post.getHashtags() != null) {
            post.setHashtags(post.getHashtags().stream()
                    .map(this::sanitizeText)
                    .toList());
        }

        if (post.getMentions() != null) {
            post.setMentions(post.getMentions().stream()
                    .map(this::sanitizeText)
                    .toList());
        }

        post.setSanitized(true);
    }

    /**
     * Sanitize text content (XSS protection).
     * <p>
     * Removes:
     * - Script tags
     * - HTML tags
     * - Dangerous characters
     * <p>
     * Escapes:
     * - HTML entities
     */
    protected String sanitizeText(String text) {
        if (text == null) {
            return null;
        }

        // Remove script tags
        text = SCRIPT_PATTERN.matcher(text).replaceAll("");

        // Remove HTML tags
        text = HTML_TAG_PATTERN.matcher(text).replaceAll("");

        // Escape HTML entities
        text = StringEscapeUtils.escapeHtml4(text);

        // Trim whitespace
        text = text.trim();

        return text;
    }

    // ==================== ABSTRACT METHODS (Platform-Specific) ====================

    /**
     * Step 0: Authenticate with platform API.
     * <p>
     * Subclasses implement platform-specific OAuth or API key authentication.
     *
     * @param authRequest authentication credentials
     * @return authentication response
     * @throws SocialConnectorException if authentication fails
     */
    protected abstract SocialAuthResponse authenticateWithPlatform(SocialAuthRequest authRequest)
            throws SocialConnectorException;

    /**
     * Step 3a: Call external API to fetch comments.
     * <p>
     * Subclasses implement platform-specific API call.
     *
     * @param postId post identifier
     * @param limit  max comments to fetch
     * @return raw API response (JSON, XML, etc.)
     * @throws SocialConnectorException if API call fails
     */
    protected abstract String callExternalApiForComments(String postId, int limit)
            throws SocialConnectorException;

    /**
     * Step 3b: Call external API to fetch posts.
     */
    protected abstract String callExternalApiForPosts(String userId, int limit)
            throws SocialConnectorException;

    /**
     * Step 3c: Call external API to fetch single post.
     */
    protected abstract String callExternalApiForPost(String postId)
            throws SocialConnectorException;

    /**
     * Step 4a: Parse API response into comments.
     * <p>
     * Subclasses implement platform-specific parsing logic.
     *
     * @param rawResponse raw API response
     * @return parsed comments
     * @throws SocialConnectorException if parsing fails
     */
    protected abstract List<SocialComment> parseCommentsResponse(String rawResponse)
            throws SocialConnectorException;

    /**
     * Step 4b: Parse API response into posts.
     */
    protected abstract List<SocialPost> parsePostsResponse(String rawResponse)
            throws SocialConnectorException;

    /**
     * Step 4c: Parse API response into single post.
     */
    protected abstract SocialPost parsePostResponse(String rawResponse)
            throws SocialConnectorException;

    /**
     * Hook method for disconnect cleanup.
     * Subclasses can override to revoke tokens or perform cleanup.
     */
    protected void performDisconnect() {
        // Default: no-op
        // Subclasses can override to revoke access tokens
    }

    /**
     * Get maximum allowed fetch limit for this platform.
     *
     * @return max limit
     */
    protected abstract int getMaxFetchLimit();

    // ==================== FUNCTIONAL INTERFACE ====================

    /**
     * Functional interface for template method operations.
     */
    @FunctionalInterface
    private interface FetchOperation<T> {
        T execute() throws Exception;
    }
}
