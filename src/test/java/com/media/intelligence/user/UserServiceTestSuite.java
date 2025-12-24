package com.media.intelligence.user;

import com.media.intelligence.user.service.UserServiceTest;
import com.media.intelligence.user_credential.service.UserCredentialServiceTest;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

/**
 * Test Suite for all Service Layer tests.
 * <p>
 * Includes:
 * - UserService (CRUD operations, validation, sanitization)
 * - UserCredentialService (password management, verification)
 * <p>
 * Total: 2 test classes, ~50 test cases
 */
@Suite
@SuiteDisplayName("Service Layer Test Suite")
@SelectClasses({
        UserServiceTest.class,
        UserCredentialServiceTest.class
})
public class UserServiceTestSuite {
    // Test suite runner - no additional code needed
}
