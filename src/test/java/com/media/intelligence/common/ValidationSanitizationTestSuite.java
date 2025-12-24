package com.media.intelligence.common;

import com.media.intelligence.common.sanitization.rule.EmailNormalizationRuleTest;
import com.media.intelligence.common.sanitization.rule.WhitespaceSanitizationRuleTest;
import com.media.intelligence.common.sanitization.rule.XssSanitizationRuleTest;
import com.media.intelligence.common.validation.rule.EmailFormatRuleTest;
import com.media.intelligence.common.validation.rule.PasswordStrengthRuleTest;
import com.media.intelligence.common.validation.rule.RequiredRuleTest;
import com.media.intelligence.common.validation.rule.StringLengthRuleTest;
import com.media.intelligence.user.sanitization.ChangePasswordDTOSanitizerTest;
import com.media.intelligence.user.sanitization.FindUserByEmailDTOSanitizerTest;
import com.media.intelligence.user.sanitization.RegisterUserDTOSanitizerTest;
import com.media.intelligence.user.sanitization.UpdateUserProfileDTOSanitizerTest;
import com.media.intelligence.user.validation.ChangePasswordDTOValidatorTest;
import com.media.intelligence.user.validation.FindUserByEmailDTOValidatorTest;
import com.media.intelligence.user.validation.RegisterUserDTOValidatorTest;
import com.media.intelligence.user.validation.UpdateUserProfileDTOValidatorTest;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

/**
 * Test Suite for all Validation and Sanitization tests.
 * <p>
 * Includes:
 * - Validation Rules (4 classes)
 * - Sanitization Rules (3 classes)
 * - DTO Validators (4 classes)
 * - DTO Sanitizers (4 classes)
 * <p>
 * Total: 15 test classes, ~210 test cases
 */
@Suite
@SuiteDisplayName("Validation and Sanitization Test Suite")
@SelectClasses({
        // Validation Rules
        RequiredRuleTest.class,
        EmailFormatRuleTest.class,
        StringLengthRuleTest.class,
        PasswordStrengthRuleTest.class,

        // Sanitization Rules
        WhitespaceSanitizationRuleTest.class,
        XssSanitizationRuleTest.class,
        EmailNormalizationRuleTest.class,

        // DTO Validators
        RegisterUserDTOValidatorTest.class,
        UpdateUserProfileDTOValidatorTest.class,
        ChangePasswordDTOValidatorTest.class,
        FindUserByEmailDTOValidatorTest.class,

        // DTO Sanitizers
        RegisterUserDTOSanitizerTest.class,
        UpdateUserProfileDTOSanitizerTest.class,
        ChangePasswordDTOSanitizerTest.class,
        FindUserByEmailDTOSanitizerTest.class
})
public class ValidationSanitizationTestSuite {
    // Test suite runner - no additional code needed
}
