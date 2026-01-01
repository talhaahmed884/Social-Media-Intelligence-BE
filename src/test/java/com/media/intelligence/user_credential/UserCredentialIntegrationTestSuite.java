package com.media.intelligence.user_credential;

import com.media.intelligence.user_credential.repository.UserCredentialRepositoryIntegrationTest;
import com.media.intelligence.user_credential.service.UserCredentialServiceIntegrationTest;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration Test Suite for UserCredential Domain.
 * <p>
 * Tests the UserCredential domain with real database (H2 in-memory).
 * Includes:
 * - Repository integration tests
 * - Service integration tests
 * <p>
 * These tests verify the entire stack from service layer to database.
 */
@ActiveProfiles("test")
@Suite
@SuiteDisplayName("UserCredential Domain - Integration Tests")
@SelectClasses({
        UserCredentialRepositoryIntegrationTest.class,
        UserCredentialServiceIntegrationTest.class
})
public class UserCredentialIntegrationTestSuite {
    // Integration test suite for UserCredential domain - no additional code needed
}
