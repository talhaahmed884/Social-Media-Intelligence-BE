package com.media.intelligence.social_connector.dto;

import com.media.intelligence.social_connector.provider.SocialPlatform;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO representing a sanitized social media comment.
 * <p>
 * This is our domain model for comments, independent of external API formats.
 * Adapters translate platform-specific comment formats into this unified structure.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SocialComment {

    /**
     * Unique comment identifier (platform-specific)
     */
    private String commentId;

    /**
     * ID of the post this comment belongs to
     */
    private String postId;

    /**
     * Source platform
     */
    private SocialPlatform platform;

    /**
     * Comment text content (sanitized for XSS)
     */
    private String text;

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
     * Number of likes/reactions
     */
    private Integer likes;

    /**
     * Number of replies to this comment
     */
    private Integer replyCount;

    /**
     * Parent comment ID (if this is a reply)
     */
    private String parentCommentId;

    /**
     * Comment creation timestamp
     */
    private LocalDateTime createdAt;

    /**
     * Whether comment has been edited
     */
    private Boolean edited;

    /**
     * Language code (e.g., "en", "es")
     */
    private String language;

    /**
     * Sentiment score (-1.0 to 1.0, if analyzed)
     */
    private Double sentimentScore;

    /**
     * Whether content has been sanitized
     */
    private boolean sanitized;
}
