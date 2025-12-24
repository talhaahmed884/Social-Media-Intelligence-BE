package com.media.intelligence.common.validation.rule;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("StringLengthRule Tests")
public class StringLengthRuleTest {

    @Test
    @DisplayName("Should pass validation when string is within bounds")
    void shouldPassWhenWithinBounds() {
        // Arrange
        StringLengthRule rule = new StringLengthRule(5, 10);
        String value = "hello";

        // Act
        boolean isValid = rule.isValid(value);

        // Assert
        assertTrue(isValid, "String within bounds should pass");
    }

    @Test
    @DisplayName("Should pass validation when string length equals minimum")
    void shouldPassWhenEqualsMinimum() {
        // Arrange
        StringLengthRule rule = new StringLengthRule(5, 10);
        String value = "hello"; // exactly 5 characters

        // Act
        boolean isValid = rule.isValid(value);

        // Assert
        assertTrue(isValid, "String at minimum length should pass");
    }

    @Test
    @DisplayName("Should pass validation when string length equals maximum")
    void shouldPassWhenEqualsMaximum() {
        // Arrange
        StringLengthRule rule = new StringLengthRule(5, 10);
        String value = "helloworld"; // exactly 10 characters

        // Act
        boolean isValid = rule.isValid(value);

        // Assert
        assertTrue(isValid, "String at maximum length should pass");
    }

    @Test
    @DisplayName("Should fail validation when string is too short")
    void shouldFailWhenTooShort() {
        // Arrange
        StringLengthRule rule = new StringLengthRule(5, 10);
        String value = "hi"; // 2 characters

        // Act
        boolean isValid = rule.isValid(value);

        // Assert
        assertFalse(isValid, "String shorter than minimum should fail");
    }

    @Test
    @DisplayName("Should fail validation when string is too long")
    void shouldFailWhenTooLong() {
        // Arrange
        StringLengthRule rule = new StringLengthRule(5, 10);
        String value = "hello world!"; // 12 characters

        // Act
        boolean isValid = rule.isValid(value);

        // Assert
        assertFalse(isValid, "String longer than maximum should fail");
    }

    @Test
    @DisplayName("Should fail validation when string is null")
    void shouldFailWhenNull() {
        // Arrange
        StringLengthRule rule = new StringLengthRule(5, 10);

        // Act
        boolean isValid = rule.isValid(null);

        // Assert
        assertFalse(isValid, "Null string should fail validation");
    }

    @Test
    @DisplayName("Should fail validation when string is empty and min > 0")
    void shouldFailWhenEmptyAndMinGreaterThanZero() {
        // Arrange
        StringLengthRule rule = new StringLengthRule(1, 10);
        String value = "";

        // Act
        boolean isValid = rule.isValid(value);

        // Assert
        assertFalse(isValid, "Empty string should fail when min > 0");
    }

    @Test
    @DisplayName("Should pass validation when string is empty and min = 0")
    void shouldPassWhenEmptyAndMinIsZero() {
        // Arrange
        StringLengthRule rule = new StringLengthRule(0, 10);
        String value = "";

        // Act
        boolean isValid = rule.isValid(value);

        // Assert
        assertTrue(isValid, "Empty string should pass when min = 0");
    }

    @Test
    @DisplayName("Should handle minimum-only validation (no maximum)")
    void shouldHandleMinimumOnly() {
        // Arrange
        StringLengthRule rule = new StringLengthRule(5, Integer.MAX_VALUE);
        String value = "hello world this is a very long string";

        // Act
        boolean isValid = rule.isValid(value);

        // Assert
        assertTrue(isValid, "Very long string should pass with no practical maximum");
    }

    @Test
    @DisplayName("Should handle exact length validation (min = max)")
    void shouldHandleExactLength() {
        // Arrange
        StringLengthRule rule = new StringLengthRule(5, 5);
        String value = "hello";

        // Act
        boolean isValid = rule.isValid(value);

        // Assert
        assertTrue(isValid, "String with exact required length should pass");
    }

    @Test
    @DisplayName("Should fail when string is one character longer than max")
    void shouldFailWhenOneCharacterTooLong() {
        // Arrange
        StringLengthRule rule = new StringLengthRule(5, 10);
        String value = "hello world"; // 11 characters

        // Act
        boolean isValid = rule.isValid(value);

        // Assert
        assertFalse(isValid, "String one character too long should fail");
    }

    @Test
    @DisplayName("Should fail when string is one character shorter than min")
    void shouldFailWhenOneCharacterTooShort() {
        // Arrange
        StringLengthRule rule = new StringLengthRule(5, 10);
        String value = "hell"; // 4 characters

        // Act
        boolean isValid = rule.isValid(value);

        // Assert
        assertFalse(isValid, "String one character too short should fail");
    }

    @Test
    @DisplayName("Should return appropriate error message")
    void shouldReturnErrorMessage() {
        // Arrange
        StringLengthRule rule = new StringLengthRule(5, 10);

        // Act
        String errorMessage = rule.getErrorMessage();

        // Assert
        assertNotNull(errorMessage, "Error message should not be null");
        assertTrue(errorMessage.contains("5") || errorMessage.contains("10"),
                "Error message should mention length constraints");
    }
}
