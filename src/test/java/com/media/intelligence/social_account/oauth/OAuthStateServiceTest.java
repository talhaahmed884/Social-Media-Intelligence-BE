package com.media.intelligence.social_account.oauth;

import com.media.intelligence.social_account.exception.SocialAccountErrorCode;
import com.media.intelligence.social_account.exception.SocialAccountException;
import com.media.intelligence.social_connector.provider.SocialPlatform;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static com.media.intelligence.social_account.SocialAccountTestFixtures.properties;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("OAuthStateService Tests")
public class OAuthStateServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final UUID USER_ID = UUID.randomUUID();

    private final OAuthStateService service = serviceAt(NOW, "secret");

    @Test
    @DisplayName("issue/verify: Should round-trip the user ID")
    void issueVerify_ShouldReturnUserId() {
        String state = service.issue(USER_ID, SocialPlatform.INSTAGRAM);

        assertEquals(USER_ID, service.verify(state, SocialPlatform.INSTAGRAM));
        assertNotEquals(state, service.issue(USER_ID, SocialPlatform.INSTAGRAM), "Each state should be unique");
    }

    @Test
    @DisplayName("verify: Should accept a state verified by another instance with the same secret")
    void verify_ShouldWorkAcrossInstances() {
        String state = service.issue(USER_ID, SocialPlatform.INSTAGRAM);

        assertEquals(USER_ID, serviceAt(NOW.plusSeconds(60), "secret").verify(state, SocialPlatform.INSTAGRAM));
    }

    @Test
    @DisplayName("verify: Should reject expired states")
    void verify_ShouldRejectExpired() {
        String state = service.issue(USER_ID, SocialPlatform.INSTAGRAM);
        OAuthStateService later = serviceAt(NOW.plus(Duration.ofMinutes(11)), "secret");

        assertInvalid(() -> later.verify(state, SocialPlatform.INSTAGRAM));
    }

    @Test
    @DisplayName("verify: Should reject a state issued for another platform")
    void verify_ShouldRejectOtherPlatform() {
        String state = service.issue(USER_ID, SocialPlatform.INSTAGRAM);

        assertInvalid(() -> service.verify(state, SocialPlatform.FACEBOOK));
    }

    @Test
    @DisplayName("verify: Should reject forged, tampered, and malformed states")
    void verify_ShouldRejectForgedStates() {
        String state = service.issue(USER_ID, SocialPlatform.INSTAGRAM);
        String[] parts = state.split("\\.");
        String otherSignature = serviceAt(NOW, "other-secret").issue(USER_ID, SocialPlatform.INSTAGRAM).split("\\.")[1];

        assertInvalid(() -> service.verify(parts[0] + "." + otherSignature, SocialPlatform.INSTAGRAM));
        assertInvalid(() -> serviceAt(NOW, "other-secret").verify(state, SocialPlatform.INSTAGRAM));
        assertInvalid(() -> service.verify("garbage", SocialPlatform.INSTAGRAM));
        assertInvalid(() -> service.verify("a.b.c", SocialPlatform.INSTAGRAM));
        assertInvalid(() -> service.verify(null, SocialPlatform.INSTAGRAM));
    }

    private static OAuthStateService serviceAt(Instant instant, String secret) {
        return new OAuthStateService(properties(null, secret), Clock.fixed(instant, ZoneOffset.UTC));
    }

    private static void assertInvalid(org.junit.jupiter.api.function.Executable executable) {
        SocialAccountException ex = assertThrows(SocialAccountException.class, executable);
        assertEquals(SocialAccountErrorCode.INVALID_OAUTH_STATE, ex.getErrorCode());
    }
}
