package com.media.intelligence.social_connector.connector.instagram;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.media.intelligence.social_connector.exception.SocialConnectorErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("InstagramErrorMapper Tests")
public class InstagramErrorMapperTest {

    private final InstagramErrorMapper mapper = new InstagramErrorMapper(new ObjectMapper());

    @ParameterizedTest(name = "HTTP {0}, code {1}, subcode {2} -> {3}")
    @DisplayName("toException: Should classify Graph errors by body code before HTTP status")
    @CsvSource({
            "400, 190, -1,  AUTHENTICATION_FAILED",
            "400, 190, 463, TOKEN_EXPIRED",
            "400, 4,   -1,  RATE_LIMIT_EXCEEDED",
            "403, 17,  -1,  RATE_LIMIT_EXCEEDED",
            "400, 80002, -1, RATE_LIMIT_EXCEEDED",
            "403, 10,  -1,  INSUFFICIENT_PERMISSIONS",
            "400, 200, -1,  INSUFFICIENT_PERMISSIONS",
            "400, 100, 33,  RESOURCE_NOT_FOUND",
            "500, 2,   -1,  PLATFORM_UNAVAILABLE",
            "400, 100, -1,  API_CALL_FAILED"
    })
    void toException_ShouldClassifyGraphErrors(int status, int code, int subcode, SocialConnectorErrorCode expected) {
        String body = String.format("{\"error\": {\"message\": \"m\", \"code\": %d, \"error_subcode\": %d}}", code, subcode);
        assertEquals(expected, mapper.toException("test", status, body).getErrorCode());
    }

    @ParameterizedTest(name = "HTTP {0} with no body -> {1}")
    @DisplayName("toException: Should fall back to HTTP status when the body has no Graph error")
    @CsvSource({
            "401, AUTHENTICATION_FAILED",
            "403, INSUFFICIENT_PERMISSIONS",
            "404, RESOURCE_NOT_FOUND",
            "429, RATE_LIMIT_EXCEEDED",
            "502, PLATFORM_UNAVAILABLE",
            "400, API_CALL_FAILED"
    })
    void toException_ShouldFallBackToStatus(int status, SocialConnectorErrorCode expected) {
        assertEquals(expected, mapper.toException("test", status, "").getErrorCode());
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("toException: Should handle transient flag and OAuth error shape")
    @CsvSource(delimiter = '|', value = {
            "{\"error\": {\"code\": 999, \"is_transient\": true}}|PLATFORM_UNAVAILABLE",
            "{\"error_type\": \"OAuthException\", \"code\": 400, \"error_message\": \"bad code\"}|INVALID_CREDENTIALS",
            "not json|API_CALL_FAILED"
    })
    void toException_ShouldHandleOtherShapes(String body, SocialConnectorErrorCode expected) {
        assertEquals(expected, mapper.toException("test", 400, body).getErrorCode());
    }
}
