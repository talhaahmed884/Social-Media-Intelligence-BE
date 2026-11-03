package com.media.intelligence.social_connector.provider;

import com.media.intelligence.social_connector.dto.SocialAuthRequest;
import com.media.intelligence.social_connector.dto.SocialAuthResponse;
import com.media.intelligence.social_connector.dto.SocialComment;
import com.media.intelligence.social_connector.dto.SocialPost;
import com.media.intelligence.social_connector.exception.SocialConnectorException;

import java.util.List;

/**
 * Core interface for Social Media integration - defines our domain contract.
 * <p>
 * **Adapter Pattern**: This interface represents the Target interface that our application
 * understands. Platform-specific implementations (InstagramAdapter, TwitterAdapter) adapt
 * external APIs to this unified interface.
 * <p>
 * **Anti-Corruption Layer**: This interface shields our domain from external API changes.
 * We control this contract, and adapters translate between external formats and our domain.
 * <p>
 * All methods throw {@link SocialConnectorException} to provide consistent error handling
 * across all platform implementations.
 *
 * @see com.media.intelligence.social_connector.connector.AbstractSocialConnector
 */
public interface SocialMediaProvider {

    /**
     * Establish connection and authenticate with the social media platform.
     * <p>
     * Validates credentials, obtains access tokens, and verifies permissions.
     *
     * @param authRequest authentication credentials and configuration
     * @return authentication response with tokens and user info
     * @throws SocialConnectorException if authentication fails
     */
    SocialAuthResponse connect(SocialAuthRequest authRequest) throws SocialConnectorException;

    /**
     * Fetch comments for a specific post.
     * <p>
     * This method implements the Template Method pattern internally,
     * executing: validate → call API → parse → sanitize.
     *
     * @param postId unique identifier of the post
     * @param limit  maximum number of comments to fetch
     * @return list of sanitized comments
     * @throws SocialConnectorException if fetch fails or rate limit exceeded
     */
    List<SocialComment> fetchComments(String postId, int limit) throws SocialConnectorException;

    /**
     * Fetch posts from a user's timeline or feed.
     *
     * @param userId user identifier (can be username or numeric ID)
     * @param limit  maximum number of posts to fetch
     * @return list of sanitized posts
     * @throws SocialConnectorException if fetch fails
     */
    List<SocialPost> fetchPosts(String userId, int limit) throws SocialConnectorException;

    /**
     * Fetch a specific post by ID.
     *
     * @param postId unique post identifier
     * @return sanitized post data
     * @throws SocialConnectorException if post not found or fetch fails
     */
    SocialPost fetchPost(String postId) throws SocialConnectorException;

    /**
     * Verify if the connection is still valid (token not expired).
     *
     * @return true if connection is valid
     */
    boolean isConnected();

    /**
     * Disconnect and invalidate credentials.
     * <p>
     * Cleans up resources, revokes tokens if possible.
     */
    void disconnect();

    /**
     * Get the platform this provider connects to.
     *
     * @return social platform enum
     */
    SocialPlatform getPlatform();

    /**
     * Get current rate limit status.
     * <p>
     * Returns remaining API calls available before hitting rate limit.
     *
     * @return remaining calls, or -1 if unlimited/unknown
     */
    int getRateLimitRemaining();
}
