package com.media.intelligence.social_connector.connector.instagram;

import com.fasterxml.jackson.databind.JsonNode;
import com.media.intelligence.social_connector.dto.SocialAuthResponse;
import com.media.intelligence.social_connector.dto.SocialComment;
import com.media.intelligence.social_connector.dto.SocialPost;
import com.media.intelligence.social_connector.dto.SocialToken;
import com.media.intelligence.social_connector.exception.SocialConnectorErrorCode;
import com.media.intelligence.social_connector.exception.SocialConnectorException;
import com.media.intelligence.social_connector.provider.SocialPlatform;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Maps Instagram JSON into our DTOs. Pure functions, no I/O.
 * <p>
 * Missing optional fields become null/0; a missing {@code id} makes the response invalid.
 * Timestamps are converted to UTC {@link LocalDateTime}; unparseable ones become null.
 */
@Slf4j
final class InstagramResponseParser {

    // Instagram timestamps look like "2023-12-31T10:30:00+0000" (UTC, no colon in offset)
    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss[xxx][xx][X]");

    private InstagramResponseParser() {
    }

    /**
     * Parse {@code GET /me?fields=user_id,username,account_type}.
     */
    static SocialAuthResponse parseProfile(JsonNode profile, String accessToken) {
        String accountId = profile.path("user_id").asText(null);
        if (accountId == null) {
            throw invalid("Instagram profile response is missing user_id");
        }

        String username = profile.path("username").asText(null);
        return SocialAuthResponse.builder()
                .authenticated(true)
                .platform(SocialPlatform.INSTAGRAM)
                .accessToken(accessToken)
                .userId(accountId)
                .username(username)
                .displayName(username)
                // /me does not report expiry; OAuth-linked tokens get it from the token endpoints
                .tokenExpiresAt(null)
                .metadata(profile.path("account_type").asText(null))
                .build();
    }

    static List<SocialComment> parseComments(JsonNode root) {
        return parseDataArray(root, InstagramResponseParser::parseComment);
    }

    static List<SocialPost> parsePosts(JsonNode root) {
        return parseDataArray(root, InstagramResponseParser::parsePost);
    }

    static SocialComment parseComment(JsonNode node) {
        String username = node.path("username").asText(null);
        return SocialComment.builder()
                .commentId(requireText(node, "id"))
                .platform(SocialPlatform.INSTAGRAM)
                .text(node.path("text").asText(null))
                .authorUsername(username)
                .authorDisplayName(username)
                .likes(node.path("like_count").asInt(0))
                .createdAt(parseTimestamp(node.path("timestamp").asText(null)))
                .build();
    }

    static SocialPost parsePost(JsonNode node) {
        List<String> mediaUrls = new ArrayList<>();
        String mediaUrl = node.path("media_url").asText(null);
        if (mediaUrl != null) {
            mediaUrls.add(mediaUrl);
        }

        String username = node.path("username").asText(null);
        return SocialPost.builder()
                .postId(requireText(node, "id"))
                .platform(SocialPlatform.INSTAGRAM)
                .caption(node.path("caption").asText(null))
                .authorId(node.path("owner").path("id").asText(null))
                .authorUsername(username)
                .authorDisplayName(username)
                .postType(node.path("media_type").asText("UNKNOWN"))
                .mediaUrls(mediaUrls)
                .likes(node.path("like_count").asInt(0))
                .commentCount(node.path("comments_count").asInt(0))
                .createdAt(parseTimestamp(node.path("timestamp").asText(null)))
                .build();
    }

    /**
     * Parse the short-lived token from {@code POST api.instagram.com/oauth/access_token}.
     * Documented shape is {@code {"data": [{"access_token", "user_id", "permissions"}]}};
     * a flat object is accepted too.
     */
    static String parseShortLivedToken(JsonNode root) {
        JsonNode tokenNode = root.path("data").isArray() ? root.path("data").path(0) : root;
        String token = tokenNode.path("access_token").asText(null);
        if (token == null) {
            throw invalid("Instagram code exchange response is missing access_token");
        }
        return token;
    }

    /**
     * Parse {@code {"access_token", "token_type", "expires_in"}} from the long-lived exchange
     * and refresh endpoints.
     *
     * @param now current UTC time, used to compute the expiry
     */
    static SocialToken parseLongLivedToken(JsonNode root, LocalDateTime now) {
        String token = root.path("access_token").asText(null);
        if (token == null) {
            throw invalid("Instagram token response is missing access_token");
        }
        long expiresIn = root.path("expires_in").asLong(0);
        return new SocialToken(token, expiresIn > 0 ? now.plusSeconds(expiresIn) : null);
    }

    /**
     * Parse Instagram timestamp to a UTC LocalDateTime.
     *
     * @return parsed timestamp, or null if missing/unparseable (never a made-up value)
     */
    static LocalDateTime parseTimestamp(String timestamp) {
        if (timestamp == null) {
            return null;
        }
        try {
            return OffsetDateTime.parse(timestamp, TIMESTAMP_FORMAT)
                    .withOffsetSameInstant(ZoneOffset.UTC)
                    .toLocalDateTime();
        } catch (DateTimeParseException e) {
            log.warn("Failed to parse Instagram timestamp: {}", timestamp);
            return null;
        }
    }

    private static <T> List<T> parseDataArray(JsonNode root, Function<JsonNode, T> mapper) {
        JsonNode dataArray = root.path("data");
        if (!dataArray.isArray()) {
            throw invalid("Instagram response has no 'data' array");
        }

        List<T> items = new ArrayList<>();
        dataArray.forEach(node -> items.add(mapper.apply(node)));
        return items;
    }

    private static String requireText(JsonNode node, String field) {
        String value = node.path(field).asText(null);
        if (value == null) {
            throw invalid("Instagram item is missing required field '" + field + "'");
        }
        return value;
    }

    private static SocialConnectorException invalid(String details) {
        return new SocialConnectorException(SocialConnectorErrorCode.API_RESPONSE_INVALID, details);
    }
}
