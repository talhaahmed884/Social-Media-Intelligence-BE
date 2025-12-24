package com.media.intelligence.common.sanitization.rule;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("WhitespaceSanitizationRule Tests")
public class WhitespaceSanitizationRuleTest {

    private WhitespaceSanitizationRule rule;

    @BeforeEach
    void setUp() {
        rule = new WhitespaceSanitizationRule();
    }

    @Test
    @DisplayName("Should trim leading whitespace")
    void shouldTrimLeadingWhitespace() {
        // Arrange
        String input = "   hello";

        // Act
        String result = rule.sanitize(input);

        // Assert
        assertEquals("hello", result, "Should remove leading whitespace");
    }

    @Test
    @DisplayName("Should trim trailing whitespace")
    void shouldTrimTrailingWhitespace() {
        // Arrange
        String input = "hello   ";

        // Act
        String result = rule.sanitize(input);

        // Assert
        assertEquals("hello", result, "Should remove trailing whitespace");
    }

    @Test
    @DisplayName("Should trim both leading and trailing whitespace")
    void shouldTrimBothSides() {
        // Arrange
        String input = "   hello   ";

        // Act
        String result = rule.sanitize(input);

        // Assert
        assertEquals("hello", result, "Should remove both leading and trailing whitespace");
    }

    @Test
    @DisplayName("Should preserve whitespace in the middle of string")
    void shouldPreserveMiddleWhitespace() {
        // Arrange
        String input = "hello world";

        // Act
        String result = rule.sanitize(input);

        // Assert
        assertEquals("hello world", result, "Should preserve whitespace in the middle");
    }

    @Test
    @DisplayName("Should handle tabs and newlines")
    void shouldHandleTabsAndNewlines() {
        // Arrange
        String input = "\t\nhello\n\t";

        // Act
        String result = rule.sanitize(input);

        // Assert
        assertEquals("hello", result, "Should remove tabs and newlines");
    }

    @Test
    @DisplayName("Should handle string with no whitespace")
    void shouldHandleNoWhitespace() {
        // Arrange
        String input = "hello";

        // Act
        String result = rule.sanitize(input);

        // Assert
        assertEquals("hello", result, "Should return same string when no whitespace");
    }

    @Test
    @DisplayName("Should return empty string when input is only whitespace")
    void shouldReturnEmptyForOnlyWhitespace() {
        // Arrange
        String input = "   ";

        // Act
        String result = rule.sanitize(input);

        // Assert
        assertEquals("", result, "Should return empty string for whitespace-only input");
    }

    @Test
    @DisplayName("Should return empty string when input is empty")
    void shouldReturnEmptyForEmptyInput() {
        // Arrange
        String input = "";

        // Act
        String result = rule.sanitize(input);

        // Assert
        assertEquals("", result, "Should return empty string for empty input");
    }

    @Test
    @DisplayName("Should return null when input is null")
    void shouldReturnNullForNullInput() {
        // Act
        String result = rule.sanitize(null);

        // Assert
        assertNull(result, "Should return null for null input");
    }

    @Test
    @DisplayName("Should handle multiple spaces")
    void shouldHandleMultipleSpaces() {
        // Arrange
        String input = "     hello world     ";

        // Act
        String result = rule.sanitize(input);

        // Assert
        assertEquals("hello world", result, "Should handle multiple spaces");
    }

    @Test
    @DisplayName("Should handle mixed whitespace characters")
    void shouldHandleMixedWhitespace() {
        // Arrange
        String input = " \t\n hello \r\n\t ";

        // Act
        String result = rule.sanitize(input);

        // Assert
        assertEquals("hello", result, "Should handle mixed whitespace characters");
    }
}
