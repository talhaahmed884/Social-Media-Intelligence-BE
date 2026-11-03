package com.media.intelligence.social_account;

import com.media.intelligence.social_account.controller.SocialAccountControllerIntegrationTest;
import com.media.intelligence.social_account.crypto.TokenCipherTest;
import com.media.intelligence.social_account.oauth.OAuthStateServiceTest;
import com.media.intelligence.social_account.repository.SocialAccountRepositoryIntegrationTest;
import com.media.intelligence.social_account.service.SocialAccountServiceTest;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

/**
 * Test Suite for the Social Account domain.
 * <p>
 * Includes:
 * - Token encryption and OAuth state signing (unit)
 * - Service logic with mocked connectors (unit)
 * - Repository queries and constraints (H2)
 * - Controller full stack with the connector factory mocked (H2)
 */
@Suite
@SuiteDisplayName("Social Account Test Suite")
@SelectClasses({
        TokenCipherTest.class,
        OAuthStateServiceTest.class,
        SocialAccountServiceTest.class,
        SocialAccountRepositoryIntegrationTest.class,
        SocialAccountControllerIntegrationTest.class
})
public class SocialAccountTestSuite {
    // Test suite runner - no additional code needed
}
