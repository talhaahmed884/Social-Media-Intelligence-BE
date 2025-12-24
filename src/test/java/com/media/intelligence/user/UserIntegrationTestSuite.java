package com.media.intelligence.user;

import com.media.intelligence.user.controller.UserControllerIntegrationTest;
import com.media.intelligence.user.controller.UserControllerTest;
import com.media.intelligence.user.repository.UserRepositoryIntegrationTest;
import com.media.intelligence.user.service.UserServiceIntegrationTest;
import com.media.intelligence.user.service.UserServiceTest;
import com.media.intelligence.user_credential.service.UserCredentialServiceTest;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration Test Suite for User Domain.
 * <p>
 * Tests the User domain with real database (H2 in-memory).
 * Includes:
 * - Repository integration tests
 * - Service integration tests
 * - Controller integration tests (full stack)
 * <p>
 * These tests verify the entire stack from HTTP requests to database.
 */
@ActiveProfiles("test")
@Suite
@SuiteDisplayName("User Domain - Integration Tests")
@SelectClasses({
        UserControllerIntegrationTest.class,
        UserControllerTest.class,
        UserRepositoryIntegrationTest.class,
        UserServiceIntegrationTest.class,
        UserServiceTest.class,
        UserCredentialServiceTest.class
})
public class UserIntegrationTestSuite {
    // Integration test suite for User domain - no additional code needed
}
