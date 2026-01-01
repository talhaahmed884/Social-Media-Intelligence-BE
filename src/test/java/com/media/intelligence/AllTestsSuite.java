package com.media.intelligence;

import com.media.intelligence.common.ValidationSanitizationTestSuite;
import com.media.intelligence.user.UserIntegrationTestSuite;
import com.media.intelligence.user_credential.UserCredentialIntegrationTestSuite;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

/**
 * Master Test Suite - Runs all test suites in the application.
 * <p>
 * Includes:
 * - Validation & Sanitization Test Suite (15 classes, ~210 tests)
 * - Service Layer Test Suite (2 classes, ~50 tests)
 * - Controller Layer Test Suite (1 class, ~20 tests)
 * - Integration Tests Suite (4 classes, ~100+ tests)
 * <p>
 * Total: ~22+ test classes, ~380+ test cases
 * <p>
 * Usage:
 * - Run this suite to execute all tests in the application
 * - Individual suites can be run separately for targeted testing
 */
@Suite
@SuiteDisplayName("Social Media Intelligence - All Tests")
@SelectClasses({
        ValidationSanitizationTestSuite.class,
        UserIntegrationTestSuite.class,
        UserCredentialIntegrationTestSuite.class
})
public class AllTestsSuite {
    // Master test suite runner - no additional code needed
}
