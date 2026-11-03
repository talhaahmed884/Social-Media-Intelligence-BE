package com.media.intelligence.social_connector.connector;

import com.media.intelligence.common.sanitization.core.SanitizationCombiner;
import com.media.intelligence.common.sanitization.rule.WhitespaceSanitizationRule;
import com.media.intelligence.common.sanitization.rule.XssSanitizationRule;
import com.media.intelligence.social_connector.dto.SocialAuthRequest;
import com.media.intelligence.social_connector.dto.SocialAuthResponse;
import com.media.intelligence.social_connector.dto.SocialComment;
import com.media.intelligence.social_connector.dto.SocialCredentials;
import com.media.intelligence.social_connector.dto.SocialPost;
import com.media.intelligence.social_connector.exception.SocialConnectorErrorCode;
import com.media.intelligence.social_connector.exception.SocialConnectorException;
import com.media.intelligence.social_connector.provider.SocialMediaProvider;
import com.media.intelligence.social_connector.resilience.RateLimiter;
import com.media.intelligence.social_connector.resilience.RetryPolicy;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Abstract base class for all social media connectors.
 * <p>
 * **Template Method Pattern**: Defines the skeleton of the data fetching algorithm.
 * Subclasses override specific steps while the overall flow remains fixed.
 * <p>
 * **Fixed Algorithm Flow** (see {@link #fetch}):
 * 1. validate credentials and arguments
 * 2. per attempt: check rate limit, then call external API (abstract - subclass implements),
 * retried by {@link RetryPolicy} on transient errors
 * 3. parse response (abstract - subclass implements)
 * 4. sanitize data (common XSS + whitespace rules)
 * <p>
 * **Stateless**: no per-account state is kept, so subclasses can be Spring singletons.
 * <p>
 * **Anti-Corruption Layer**: This class shields our domain from external API specifics.
 * Platform-specific logic is delegated to concrete adapter implementations.
 *
 * @param <R> raw response type returned by the platform API calls (e.g. Jackson JsonNode)
 */
@Slf4j
public abstract class AbstractSocialConnector<R> implements SocialMediaProvider {

    // Same rules the user domain uses; text is cleaned of HTML but not HTML-escaped
    private static final SanitizationCombiner<String> TEXT_SANITIZER = new SanitizationCombiner<>(
            new XssSanitizationRule(),
            new WhitespaceSanitizationRule()
    );

    protected final RateLimiter rateLimiter;
    protected final RetryPolicy retryPolicy;

    protected AbstractSocialConnector(RateLimiter rateLimiter, RetryPolicy retryPolicy) {
        this.rateLimiter = rateLimiter;
        this.retryPolicy = retryPolicy;
    }

    // ==================== PUBLIC API METHODS ====================

    @Override
    public SocialAuthResponse connect(SocialAuthRequest authRequest) throws SocialConnectorException {
        validateAuthRequest(authRequest);
        log.info("Connecting to {} platform", getPlatform().getDisplayName());

        SocialAuthResponse authResponse = execute(() -> retryPolicy.execute(() -> authenticateWithPlatform(authRequest)));

        if (authResponse == null || !authResponse.isAuthenticated()) {
            throw new SocialConnectorException(
                    SocialConnectorErrorCode.AUTHENTICATION_FAILED,
                    "Authentication failed for " + getPlatform().getDisplayName()
            );
        }

        log.info("Successfully connected to {} as user: {}",
                getPlatform().getDisplayName(), authResponse.getUsername());
        return authResponse;
    }

    @Override
    public List<SocialComment> fetchComments(SocialCredentials credentials, String postId, int limit)
            throws SocialConnectorException {
        requireNonBlank(postId, "Post ID");
        validateLimit(limit);

        List<SocialComment> comments = fetch(credentials,
                () -> callExternalApiForComments(credentials, postId, limit),
                this::parseCommentsResponse);

        comments.forEach(comment -> {
            if (comment.getPostId() == null) {
                comment.setPostId(postId);
            }
            sanitizeComment(comment);
        });

        log.info("Fetched {} comments for post {} from {}", comments.size(), postId, getPlatform().getDisplayName());
        return comments;
    }

    @Override
    public List<SocialPost> fetchPosts(SocialCredentials credentials, String userId, int limit)
            throws SocialConnectorException {
        requireNonBlank(userId, "User ID");
        validateLimit(limit);

        List<SocialPost> posts = fetch(credentials,
                () -> callExternalApiForPosts(credentials, userId, limit),
                this::parsePostsResponse);

        posts.forEach(this::sanitizePost);

        log.info("Fetched {} posts for user {} from {}", posts.size(), userId, getPlatform().getDisplayName());
        return posts;
    }

    @Override
    public SocialPost fetchPost(SocialCredentials credentials, String postId) throws SocialConnectorException {
        requireNonBlank(postId, "Post ID");

        SocialPost post = fetch(credentials,
                () -> callExternalApiForPost(credentials, postId),
                this::parsePostResponse);

        sanitizePost(post);

        log.info("Fetched post {} from {}", postId, getPlatform().getDisplayName());
        return post;
    }

    @Override
    public int getRateLimitRemaining(String accountId) {
        return rateLimiter.getRemainingRequests(getPlatform(), accountId);
    }

    // ==================== TEMPLATE METHOD ====================

    /**
     * Shared fetch algorithm: validate credentials → (rate limit + API call) with retry → parse.
     * <p>
     * The rate limit is checked inside the retry loop so every real HTTP call is counted.
     *
     * @param credentials account credentials
     * @param apiCall     platform-specific API call returning the raw response
     * @param parser      platform-specific parser for the raw response
     * @param <T>         parsed result type
     * @return parsed (not yet sanitized) result
     */
    protected <T> T fetch(SocialCredentials credentials, Supplier<R> apiCall, Function<R, T> parser) {
        validateCredentials(credentials);

        return execute(() -> {
            R rawResponse = retryPolicy.execute(() -> {
                rateLimiter.checkRateLimit(getPlatform(), credentials.accountId());
                return apiCall.get();
            });
            return parser.apply(rawResponse);
        });
    }

    /**
     * Run an operation and translate unexpected runtime exceptions into our domain exception.
     */
    protected <T> T execute(Supplier<T> operation) {
        try {
            return operation.get();
        } catch (SocialConnectorException e) {
            throw e;
        } catch (RuntimeException e) {
            log.error("Unexpected error during {} operation: {}", getPlatform().getDisplayName(), e.getMessage(), e);
            throw new SocialConnectorException(
                    SocialConnectorErrorCode.OPERATION_FAILED,
                    "Unexpected error during " + getPlatform().getDisplayName() + " operation",
                    e
            );
        }
    }

    // ==================== VALIDATION ====================

    /**
     * Validate the credentials passed to a fetch call.
     *
     * @throws SocialConnectorException if missing, for another platform, or expired
     */
    protected void validateCredentials(SocialCredentials credentials) throws SocialConnectorException {
        if (credentials == null) {
            throw new SocialConnectorException(
                    SocialConnectorErrorCode.INVALID_CREDENTIALS,
                    "Credentials are required"
            );
        }

        if (credentials.platform() != getPlatform()) {
            throw new SocialConnectorException(
                    SocialConnectorErrorCode.CONFIGURATION_ERROR,
                    "Platform mismatch: expected " + getPlatform() + ", got " + credentials.platform()
            );
        }

        if (isBlank(credentials.accessToken()) || isBlank(credentials.accountId())) {
            throw new SocialConnectorException(
                    SocialConnectorErrorCode.INVALID_CREDENTIALS,
                    "Access token and account ID are required"
            );
        }

        if (credentials.isExpired()) {
            throw new SocialConnectorException(
                    SocialConnectorErrorCode.TOKEN_EXPIRED,
                    "Access token expired for " + getPlatform().getDisplayName() + " account " + credentials.accountId()
            );
        }
    }

    /**
     * Validate auth request.
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

        if (isBlank(request.getAccessToken()) && isBlank(request.getApiKey())) {
            throw new SocialConnectorException(
                    SocialConnectorErrorCode.INVALID_CREDENTIALS,
                    "Access token or API key required"
            );
        }
    }

    /**
     * Validate a required identifier argument.
     */
    protected void requireNonBlank(String value, String name) throws SocialConnectorException {
        if (isBlank(value)) {
            throw new SocialConnectorException(
                    SocialConnectorErrorCode.DATA_VALIDATION_FAILED,
                    name + " cannot be null or empty"
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

    protected static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    // ==================== SANITIZATION ====================

    /**
     * Sanitize single comment.
     */
    protected void sanitizeComment(SocialComment comment) {
        if (comment == null) return;

        comment.setText(sanitizeText(comment.getText()));
        comment.setAuthorUsername(sanitizeText(comment.getAuthorUsername()));
        comment.setAuthorDisplayName(sanitizeText(comment.getAuthorDisplayName()));
        comment.setSanitized(true);
    }

    /**
     * Sanitize single post.
     */
    protected void sanitizePost(SocialPost post) {
        if (post == null) return;

        post.setCaption(sanitizeText(post.getCaption()));
        post.setAuthorUsername(sanitizeText(post.getAuthorUsername()));
        post.setAuthorDisplayName(sanitizeText(post.getAuthorDisplayName()));
        post.setLocation(sanitizeText(post.getLocation()));

        if (post.getHashtags() != null) {
            post.setHashtags(post.getHashtags().stream().map(this::sanitizeText).toList());
        }

        if (post.getMentions() != null) {
            post.setMentions(post.getMentions().stream().map(this::sanitizeText).toList());
        }

        post.setSanitized(true);
    }

    /**
     * Sanitize text content: strip HTML/script (jsoup) and trim whitespace.
     * Plain text is kept as-is (not HTML-escaped); escape at render time.
     */
    protected String sanitizeText(String text) {
        return TEXT_SANITIZER.sanitize(text);
    }

    // ==================== ABSTRACT METHODS (Platform-Specific) ====================

    /**
     * Authenticate with platform API and return the account the credentials belong to.
     *
     * @param authRequest authentication credentials
     * @return authentication response
     * @throws SocialConnectorException if authentication fails
     */
    protected abstract SocialAuthResponse authenticateWithPlatform(SocialAuthRequest authRequest)
            throws SocialConnectorException;

    /**
     * Call external API to fetch comments.
     * <p>
     * Must throw {@link SocialConnectorException} with an accurate error code; retry decisions
     * are based on {@link SocialConnectorErrorCode#isRetryable()}.
     *
     * @return raw API response
     */
    protected abstract R callExternalApiForComments(SocialCredentials credentials, String postId, int limit)
            throws SocialConnectorException;

    /**
     * Call external API to fetch posts.
     */
    protected abstract R callExternalApiForPosts(SocialCredentials credentials, String userId, int limit)
            throws SocialConnectorException;

    /**
     * Call external API to fetch single post.
     */
    protected abstract R callExternalApiForPost(SocialCredentials credentials, String postId)
            throws SocialConnectorException;

    /**
     * Parse API response into comments.
     */
    protected abstract List<SocialComment> parseCommentsResponse(R rawResponse)
            throws SocialConnectorException;

    /**
     * Parse API response into posts.
     */
    protected abstract List<SocialPost> parsePostsResponse(R rawResponse)
            throws SocialConnectorException;

    /**
     * Parse API response into single post.
     */
    protected abstract SocialPost parsePostResponse(R rawResponse)
            throws SocialConnectorException;

    /**
     * Get maximum allowed fetch limit for this platform.
     *
     * @return max limit
     */
    protected abstract int getMaxFetchLimit();
}
