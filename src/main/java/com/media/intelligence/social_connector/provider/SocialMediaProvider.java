package com.media.intelligence.social_connector.provider;

import com.media.intelligence.social_connector.dto.SocialAuthRequest;
import com.media.intelligence.social_connector.dto.SocialAuthResponse;
import com.media.intelligence.social_connector.dto.SocialComment;
import com.media.intelligence.social_connector.dto.SocialCredentials;
import com.media.intelligence.social_connector.dto.SocialPost;
import com.media.intelligence.social_connector.exception.SocialConnectorException;

import java.util.List;

/**
 * Core interface for Social Media integration - defines our domain contract.
 * <p>
 * **Adapter Pattern**: This interface represents the Target interface that our application
 * understands. Platform-specific implementations (InstagramConnector, ...) adapt
 * external APIs to this unified interface.
 * <p>
 * **Anti-Corruption Layer**: This interface shields our domain from external API changes.
 * We control this contract, and adapters translate between external formats and our domain.
 * <p>
 * **Stateless**: implementations are thread-safe Spring singletons and hold no per-account state.
 * Callers pass {@link SocialCredentials} on every call, so one instance serves all users.
 * <p>
 * All methods throw {@link SocialConnectorException} to provide consistent error handling
 * across all platform implementations.
 *
 * @see com.media.intelligence.social_connector.connector.AbstractSocialConnector
 */
public interface SocialMediaProvider {

    /**
     * Get the platform this provider connects to.
     *
     * @return social platform enum
     */
    SocialPlatform getPlatform();

    /**
     * Verify credentials with the platform and return the account they belong to.
     * <p>
     * Use {@link SocialCredentials#from(SocialAuthResponse)} to turn the result into
     * credentials for the fetch methods (and persist them for later use).
     *
     * @param authRequest authentication credentials and configuration
     * @return authentication response with account info
     * @throws SocialConnectorException if authentication fails
     */
    SocialAuthResponse connect(SocialAuthRequest authRequest) throws SocialConnectorException;

    /**
     * Fetch comments for a specific post.
     * <p>
     * This method implements the Template Method pattern internally,
     * executing: validate → rate limit → call API (with retry) → parse → sanitize.
     *
     * @param credentials credentials of the account making the request
     * @param postId      unique identifier of the post
     * @param limit       maximum number of comments to fetch
     * @return list of sanitized comments
     * @throws SocialConnectorException if fetch fails or rate limit exceeded
     */
    List<SocialComment> fetchComments(SocialCredentials credentials, String postId, int limit)
            throws SocialConnectorException;

    /**
     * Fetch posts from a user's timeline or feed.
     *
     * @param credentials credentials of the account making the request
     * @param userId      platform user identifier whose posts to fetch
     * @param limit       maximum number of posts to fetch
     * @return list of sanitized posts
     * @throws SocialConnectorException if fetch fails
     */
    List<SocialPost> fetchPosts(SocialCredentials credentials, String userId, int limit)
            throws SocialConnectorException;

    /**
     * Fetch a specific post by ID.
     *
     * @param credentials credentials of the account making the request
     * @param postId      unique post identifier
     * @return sanitized post data
     * @throws SocialConnectorException if post not found or fetch fails
     */
    SocialPost fetchPost(SocialCredentials credentials, String postId) throws SocialConnectorException;

    /**
     * Get the remaining calls in the local rate-limit window for an account.
     *
     * @param accountId platform account ID
     * @return remaining calls before the local limiter blocks requests
     */
    int getRateLimitRemaining(String accountId);
}
