package com.media.intelligence.social_connector.dto;

import com.media.intelligence.social_connector.provider.SocialPlatform;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO representing a sanitized social media post.
 * <p>
 * Unified domain model for posts across all platforms.
 * Platform-specific fields are normalized into this structure by adapters.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SocialPost {

    /**
     * Unique post identifier (platform-specific)
     */
    private String postId;

    /**
     * Source platform
     */
    private SocialPlatform platform;

    /**
     * Post caption/text content (sanitized)
     */
    private String caption;

    /**
     * Author's user ID
     */
    private String authorId;

    /**
     * Author's username/handle
     */
    private String authorUsername;

    /**
     * Author's display name
     */
    private String authorDisplayName;

    /**
     * Post type (photo, video, carousel, reel, etc.)
     */
    private String postType;

    /**
     * Media URLs (images, videos)
     */
    private List<String> mediaUrls;

    /**
     * Number of likes/reactions
     */
    private Integer likes;

    /**
     * Number of comments
     */
    private Integer commentCount;

    /**
     * Number of shares/retweets
     */
    private Integer shares;

    /**
     * Number of views (for videos)
     */
    private Integer views;

    /**
     * Post creation timestamp
     */
    private LocalDateTime createdAt;

    /**
     * Hashtags used in the post
     */
    private List<String> hashtags;

    /**
     * Mentioned users (@username)
     */
    private List<String> mentions;

    /**
     * Post location/place name
     */
    private String location;

    /**
     * Language code
     */
    private String language;

    /**
     * Whether content has been sanitized
     */
    private boolean sanitized;

    /**
     * Engagement rate (calculated if applicable)
     */
    private Double engagementRate;
}
