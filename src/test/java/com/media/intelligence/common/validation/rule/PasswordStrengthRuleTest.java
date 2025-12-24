package com.media.intelligence.common.validation.rule;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PasswordStrengthRule Tests")
public class PasswordStrengthRuleTest {

    @Test
    @DisplayName("WEAK: Should pass for passwords >= 6 characters")
    void weakShouldPassForSixCharacters() {
        // Arrange
        PasswordStrengthRule rule = PasswordStrengthRule.basic();
        String password = "123456";

        // Act
        boolean isValid = rule.isValid(password);

        // Assert
        assertTrue(isValid, "6-character password should pass WEAK validation");
    }

    @Test
    @DisplayName("WEAK: Should fail for passwords < 6 characters")
    void weakShouldFailForLessThanSixCharacters() {
        // Arrange
        PasswordStrengthRule rule = PasswordStrengthRule.basic();
        String password = "12345";

        // Act
        boolean isValid = rule.isValid(password);

        // Assert
        assertFalse(isValid, "5-character password should fail WEAK validation");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Password1",
            "Test1234",
            "MyPass99",
            "abcDEF123",
            "Secure12"
    })
    @DisplayName("MEDIUM: Should pass for passwords with uppercase, lowercase, and digit")
    void mediumShouldPassForValidPasswords(String password) {
        // Arrange
        PasswordStrengthRule rule = PasswordStrengthRule.medium();

        // Act
        boolean isValid = rule.isValid(password);

        // Assert
        assertTrue(isValid, "Password with mixed case and digit should pass MEDIUM: " + password);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "password",      // no uppercase, no digit
            "PASSWORD123",   // no lowercase
            "Passwor",      // no digit
            "12345678",      // no letters
            "Pass1"          // too short
    })
    @DisplayName("MEDIUM: Should fail for passwords missing required character types")
    void mediumShouldFailForInvalidPasswords(String password) {
        // Arrange
        PasswordStrengthRule rule = PasswordStrengthRule.medium();

        // Act
        boolean isValid = rule.isValid(password);

        // Assert
        assertFalse(isValid, "Password missing required character types should fail MEDIUM: " + password);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Password1!",
            "Secure@123",
            "MyP@ss99",
            "Test#1234",
            "Strong$Pass1"
    })
    @DisplayName("STRONG: Should pass for passwords with uppercase, lowercase, digit, and special char")
    void strongShouldPassForValidPasswords(String password) {
        // Arrange
        PasswordStrengthRule rule = PasswordStrengthRule.strong();

        // Act
        boolean isValid = rule.isValid(password);

        // Assert
        assertTrue(isValid, "Strong password should pass STRONG validation: " + password);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Password1",     // no special char
            "password1!",    // no uppercase
            "PASSWORD1!",    // no lowercase
            "Password!",     // no digit
            "Pass1!",        // too short (7 chars, need 8+)
            "12345678!",     // no letters
            "Abcdefgh1"      // no special char
    })
    @DisplayName("STRONG: Should fail for passwords missing required character types or too short")
    void strongShouldFailForInvalidPasswords(String password) {
        // Arrange
        PasswordStrengthRule rule = PasswordStrengthRule.strong();

        // Act
        boolean isValid = rule.isValid(password);

        // Assert
        assertFalse(isValid, "Weak password should fail STRONG validation: " + password);
    }

    @Test
    @DisplayName("Should fail for null password")
    void shouldFailForNullPassword() {
        // Arrange
        PasswordStrengthRule weakRule = PasswordStrengthRule.basic();
        PasswordStrengthRule mediumRule = PasswordStrengthRule.medium();
        PasswordStrengthRule strongRule = PasswordStrengthRule.strong();

        // Act & Assert
        assertFalse(weakRule.isValid(null), "WEAK should fail for null");
        assertFalse(mediumRule.isValid(null), "MEDIUM should fail for null");
        assertFalse(strongRule.isValid(null), "STRONG should fail for null");
    }

    @Test
    @DisplayName("Should fail for empty password")
    void shouldFailForEmptyPassword() {
        // Arrange
        PasswordStrengthRule weakRule = PasswordStrengthRule.basic();
        PasswordStrengthRule mediumRule = PasswordStrengthRule.medium();
        PasswordStrengthRule strongRule = PasswordStrengthRule.strong();
        String password = "";

        // Act & Assert
        assertFalse(weakRule.isValid(password), "WEAK should fail for empty");
        assertFalse(mediumRule.isValid(password), "MEDIUM should fail for empty");
        assertFalse(strongRule.isValid(password), "STRONG should fail for empty");
    }

    @Test
    @DisplayName("STRONG: Should handle various special characters")
    void strongShouldHandleVariousSpecialCharacters() {
        // Arrange
        PasswordStrengthRule rule = PasswordStrengthRule.strong();
        String[] passwords = {
                "Password1!",
                "Password1@",
                "Password1#",
                "Password1$",
                "Password1%",
                "Password1^",
                "Password1&",
                "Password1*"
        };

        // Act & Assert
        for (String password : passwords) {
            assertTrue(rule.isValid(password),
                    "Password with special char should pass: " + password);
        }
    }

    @Test
    @DisplayName("Should return appropriate error message for each strength level")
    void shouldReturnAppropriateErrorMessages() {
        // Arrange
        PasswordStrengthRule weakRule = PasswordStrengthRule.basic();
        PasswordStrengthRule mediumRule = PasswordStrengthRule.medium();
        PasswordStrengthRule strongRule = PasswordStrengthRule.strong();

        // Act
        String weakMessage = weakRule.getErrorMessage();
        String mediumMessage = mediumRule.getErrorMessage();
        String strongMessage = strongRule.getErrorMessage();

        // Assert
        assertNotNull(weakMessage, "WEAK error message should not be null");
        assertNotNull(mediumMessage, "MEDIUM error message should not be null");
        assertNotNull(strongMessage, "STRONG error message should not be null");

        assertTrue(weakMessage.toLowerCase().contains("password") ||
                        weakMessage.toLowerCase().contains("character"),
                "WEAK message should mention password or characters");
        assertTrue(mediumMessage.toLowerCase().contains("password"),
                "MEDIUM message should mention password");
        assertTrue(strongMessage.toLowerCase().contains("password"),
                "STRONG message should mention password");
    }

    @Test
    @DisplayName("STRONG: Minimum length is 8 characters")
    void strongMinimumLengthIsEight() {
        // Arrange
        PasswordStrengthRule rule = PasswordStrengthRule.strong();
        String sevenChars = "Pass1!a";  // 7 chars - all requirements except length
        String eightChars = "Pass1!ab"; // 8 chars - all requirements

        // Act & Assert
        assertFalse(rule.isValid(sevenChars), "7 characters should fail STRONG");
        assertTrue(rule.isValid(eightChars), "8 characters should pass STRONG");
    }

    @Test
    @DisplayName("MEDIUM: Minimum length is 6 characters")
    void mediumMinimumLengthIsSix() {
        // Arrange
        PasswordStrengthRule rule = PasswordStrengthRule.medium();
        String fiveChars = "Pas1";    // 5 chars
        String eightChars = "Pass1wor";   // 8 chars

        // Act & Assert
        assertFalse(rule.isValid(fiveChars), "5 characters should fail MEDIUM");
        assertTrue(rule.isValid(eightChars), "6 characters should pass MEDIUM");
    }
}
