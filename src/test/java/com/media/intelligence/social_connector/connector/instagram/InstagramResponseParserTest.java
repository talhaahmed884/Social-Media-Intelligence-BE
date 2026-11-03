package com.media.intelligence.social_connector.connector.instagram;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.media.intelligence.social_connector.dto.SocialToken;
import com.media.intelligence.social_connector.exception.SocialConnectorErrorCode;
import com.media.intelligence.social_connector.exception.SocialConnectorException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("InstagramResponseParser Tests")
public class InstagramResponseParserTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("parseTimestamp: Should handle Instagram, ISO, and invalid formats")
    void parseTimestamp_ShouldHandleFormats() {
        LocalDateTime expected = LocalDateTime.of(2024, 1, 15, 10, 30);

        assertEquals(expected, InstagramResponseParser.parseTimestamp("2024-01-15T10:30:00+0000"));
        assertEquals(expected, InstagramResponseParser.parseTimestamp("2024-01-15T10:30:00+00:00"));
        assertEquals(expected, InstagramResponseParser.parseTimestamp("2024-01-15T10:30:00Z"));
        assertEquals(expected, InstagramResponseParser.parseTimestamp("2024-01-15T12:30:00+0200"), "Should convert to UTC");
        assertNull(InstagramResponseParser.parseTimestamp("not-a-date"));
        assertNull(InstagramResponseParser.parseTimestamp(null));
    }

    @Test
    @DisplayName("parseShortLivedToken: Should accept the documented data-wrapped shape and a flat shape")
    void parseShortLivedToken_ShouldAcceptBothShapes() throws Exception {
        assertEquals("a", InstagramResponseParser.parseShortLivedToken(
                json("{\"data\": [{\"access_token\": \"a\", \"user_id\": \"1\"}]}")));
        assertEquals("b", InstagramResponseParser.parseShortLivedToken(
                json("{\"access_token\": \"b\", \"user_id\": \"1\"}")));
    }

    @Test
    @DisplayName("parseShortLivedToken: Should fail when access_token is missing")
    void parseShortLivedToken_ShouldFailWhenMissing() throws Exception {
        JsonNode body = json("{\"data\": []}");

        SocialConnectorException ex = assertThrows(SocialConnectorException.class, () ->
                InstagramResponseParser.parseShortLivedToken(body));
        assertEquals(SocialConnectorErrorCode.API_RESPONSE_INVALID, ex.getErrorCode());
    }

    @Test
    @DisplayName("parseLongLivedToken: Should compute expiry from expires_in, or leave it null")
    void parseLongLivedToken_ShouldComputeExpiry() throws Exception {
        LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0);

        SocialToken withExpiry = InstagramResponseParser.parseLongLivedToken(
                json("{\"access_token\": \"t\", \"expires_in\": 86400}"), now);
        SocialToken withoutExpiry = InstagramResponseParser.parseLongLivedToken(
                json("{\"access_token\": \"t\"}"), now);

        assertEquals(now.plusDays(1), withExpiry.expiresAt());
        assertNull(withoutExpiry.expiresAt());
    }

    @Test
    @DisplayName("parsePosts: Should fail with API_RESPONSE_INVALID when data array is missing or item has no id")
    void parsePosts_ShouldRejectInvalidResponses() throws Exception {
        JsonNode noData = json("{}");
        JsonNode noId = json("{\"data\": [{\"caption\": \"x\"}]}");

        assertEquals(SocialConnectorErrorCode.API_RESPONSE_INVALID, assertThrows(SocialConnectorException.class, () ->
                InstagramResponseParser.parsePosts(noData)).getErrorCode());
        assertEquals(SocialConnectorErrorCode.API_RESPONSE_INVALID, assertThrows(SocialConnectorException.class, () ->
                InstagramResponseParser.parsePosts(noId)).getErrorCode());
    }

    @Test
    @DisplayName("parseComment: Should default optional fields")
    void parseComment_ShouldDefaultOptionalFields() throws Exception {
        var comment = InstagramResponseParser.parseComment(json("{\"id\": \"c1\"}"));

        assertEquals("c1", comment.getCommentId());
        assertNull(comment.getText());
        assertNull(comment.getCreatedAt());
        assertEquals(0, comment.getLikes());
    }

    private JsonNode json(String value) throws Exception {
        return objectMapper.readTree(value);
    }
}
