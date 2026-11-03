package com.media.intelligence.social_account;

import com.media.intelligence.social_account.config.SocialAccountProperties;

import java.time.Duration;

/**
 * Shared test values for the social account domain.
 */
public final class SocialAccountTestFixtures {

    /**
     * Base64 of the 32 ASCII bytes "test-key-test-key-test-key-12345". Test use only.
     */
    public static final String TEST_ENCRYPTION_KEY = "dGVzdC1rZXktdGVzdC1rZXktdGVzdC1rZXktMTIzNDU=";

    private SocialAccountTestFixtures() {
    }

    public static SocialAccountProperties properties(String encryptionKey, String stateSecret) {
        return new SocialAccountProperties(
                encryptionKey,
                new SocialAccountProperties.OAuth(stateSecret, Duration.ofMinutes(10)),
                new SocialAccountProperties.TokenRefresh(true, "0 0 3 * * *", Duration.ofDays(10), Duration.ofHours(24))
        );
    }

    public static SocialAccountProperties properties() {
        return properties(TEST_ENCRYPTION_KEY, "test-state-secret");
    }
}
