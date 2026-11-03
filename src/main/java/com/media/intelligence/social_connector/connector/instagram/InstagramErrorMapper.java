package com.media.intelligence.social_connector.connector.instagram;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.media.intelligence.social_connector.exception.SocialConnectorErrorCode;
import com.media.intelligence.social_connector.exception.SocialConnectorException;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;

/**
 * Translates non-2xx Instagram responses into {@link SocialConnectorException}s.
 * <p>
 * Graph error codes in the body take precedence over the HTTP status, because Meta returns
 * e.g. invalid-token and rate-limit errors with a plain 400/403.
 * <p>
 * Two body shapes exist:
 * - Graph API: {@code {"error": {"message", "type", "code", "error_subcode", "is_transient"}}}
 * - OAuth code exchange: {@code {"error_type": "OAuthException", "code": 400, "error_message": "..."}}
 *
 * @see <a href="https://developers.facebook.com/docs/graph-api/guides/error-handling">Graph API error handling</a>
 */
@Slf4j
class InstagramErrorMapper {

    private static final int ERROR_INVALID_TOKEN = 190;
    private static final int SUBCODE_TOKEN_EXPIRED = 463;
    private static final int SUBCODE_OBJECT_NOT_FOUND = 33;
    private static final Set<Integer> RATE_LIMIT_ERRORS = Set.of(4, 17, 32, 341, 613, 80002);
    private static final Set<Integer> PERMISSION_ERRORS = Set.of(3, 10);
    private static final Set<Integer> TRANSIENT_ERRORS = Set.of(1, 2);

    private final ObjectMapper objectMapper;

    InstagramErrorMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Map an error response.
     *
     * @param operation short description of the call (never contains the token)
     * @param status    HTTP status
     * @param body      response body (may be empty)
     * @return exception to throw
     */
    SocialConnectorException toException(String operation, int status, String body) {
        JsonNode root = readTree(body);

        if (root.has("error_type")) {
            // OAuth endpoints (e.g. invalid/used authorization code)
            return build(SocialConnectorErrorCode.INVALID_CREDENTIALS, operation, status, -1, -1,
                    root.path("error_message").asText(""));
        }

        JsonNode error = root.path("error");
        int code = error.path("code").asInt(-1);
        int subcode = error.path("error_subcode").asInt(-1);
        boolean transientError = error.path("is_transient").asBoolean(false);

        return build(classify(status, code, subcode, transientError), operation, status, code, subcode,
                error.path("message").asText(""));
    }

    private SocialConnectorErrorCode classify(int status, int code, int subcode, boolean transientError) {
        if (code == ERROR_INVALID_TOKEN) {
            return subcode == SUBCODE_TOKEN_EXPIRED
                    ? SocialConnectorErrorCode.TOKEN_EXPIRED
                    : SocialConnectorErrorCode.AUTHENTICATION_FAILED;
        }
        if (RATE_LIMIT_ERRORS.contains(code)) {
            return SocialConnectorErrorCode.RATE_LIMIT_EXCEEDED;
        }
        if (PERMISSION_ERRORS.contains(code) || (code >= 200 && code <= 299)) {
            return SocialConnectorErrorCode.INSUFFICIENT_PERMISSIONS;
        }
        if (subcode == SUBCODE_OBJECT_NOT_FOUND) {
            return SocialConnectorErrorCode.RESOURCE_NOT_FOUND;
        }
        if (transientError || TRANSIENT_ERRORS.contains(code)) {
            return SocialConnectorErrorCode.PLATFORM_UNAVAILABLE;
        }
        return switch (status) {
            case 401 -> SocialConnectorErrorCode.AUTHENTICATION_FAILED;
            case 403 -> SocialConnectorErrorCode.INSUFFICIENT_PERMISSIONS;
            case 404 -> SocialConnectorErrorCode.RESOURCE_NOT_FOUND;
            case 429 -> SocialConnectorErrorCode.RATE_LIMIT_EXCEEDED;
            default -> status >= 500
                    ? SocialConnectorErrorCode.PLATFORM_UNAVAILABLE
                    : SocialConnectorErrorCode.API_CALL_FAILED;
        };
    }

    private SocialConnectorException build(SocialConnectorErrorCode errorCode, String operation,
                                           int status, int code, int subcode, String platformMessage) {
        String details = String.format("Instagram %s returned HTTP %d (code=%d, subcode=%d): %s",
                operation, status, code, subcode, platformMessage);
        log.warn(details);
        return new SocialConnectorException(errorCode, details);
    }

    private JsonNode readTree(String body) {
        if (body == null || body.isBlank()) {
            return objectMapper.missingNode();
        }
        try {
            return objectMapper.readTree(body);
        } catch (JsonProcessingException e) {
            return objectMapper.missingNode();
        }
    }
}
