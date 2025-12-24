package com.media.intelligence.user;

import com.media.intelligence.user.controller.UserControllerTest;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

/**
 * Test Suite for all Controller Layer tests.
 * <p>
 * Includes:
 * - UserController (REST endpoints, HTTP status codes, request/response handling)
 * <p>
 * Total: 1 test class, ~20 test cases
 */
@Suite
@SuiteDisplayName("Controller Layer Test Suite")
@SelectClasses({
        UserControllerTest.class
})
public class UserControllerTestSuite {
    // Test suite runner - no additional code needed
}
