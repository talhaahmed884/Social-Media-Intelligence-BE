package com.media.intelligence.common.validation.rule;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("EmailFormatRule Tests")
public class EmailFormatRuleTest {

    private EmailFormatRule rule;

    @BeforeEach
    void setUp() {
        rule = new EmailFormatRule();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "user@example.com",
            "john.doe@company.co.uk",
            "test+tag@gmail.com",
            "user_name@domain.com",
            "user123@test-domain.com",
            "a@b.co",
            "first.last@sub.domain.com"
    })
    @DisplayName("Should pass validation for valid email formats")
    void shouldPassForValidEmails(String email) {
        // Act
        boolean isValid = rule.isValid(email);

        // Assert
        assertTrue(isValid, "Valid email should pass: " + email);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "invalid",
            "@example.com",
            "user@",
            "user @example.com",
            "user@.com",
            "user..name@example.com",
            "user@domain",
            "user@domain.",
            ".user@example.com",
            "user.@example.com",
            "user@domain..com",
            "",
            "   ",
            "user name@example.com"
    })
    @DisplayName("Should fail validation for invalid email formats")
    void shouldFailForInvalidEmails(String email) {
        // Act
        boolean isValid = rule.isValid(email);

        // Assert
        assertFalse(isValid, "Invalid email should fail: " + email);
    }

    @Test
    @DisplayName("Should fail validation when email is null")
    void shouldFailWhenNull() {
        // Act
        boolean isValid = rule.isValid(null);

        // Assert
        assertFalse(isValid, "Null email should fail validation");
    }

    @Test
    @DisplayName("Should fail validation for email with multiple @ symbols")
    void shouldFailForMultipleAtSymbols() {
        // Arrange
        String email = "user@@example.com";

        // Act
        boolean isValid = rule.isValid(email);

        // Assert
        assertFalse(isValid, "Email with multiple @ symbols should fail");
    }

    @Test
    @DisplayName("Should fail validation for email with special characters")
    void shouldFailForSpecialCharacters() {
        // Arrange
        String email = "user#$%@example.com";

        // Act
        boolean isValid = rule.isValid(email);

        // Assert
        assertFalse(isValid, "Email with special characters should fail");
    }

    @Test
    @DisplayName("Should return appropriate error message")
    void shouldReturnErrorMessage() {
        // Arrange & Act
        String errorMessage = rule.getErrorMessage();

        // Assert
        assertNotNull(errorMessage, "Error message should not be null");
        assertTrue(errorMessage.toLowerCase().contains("email"),
                "Error message should mention 'email'");
    }
}
