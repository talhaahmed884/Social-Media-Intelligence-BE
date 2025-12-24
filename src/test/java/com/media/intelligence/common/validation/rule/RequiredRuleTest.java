package com.media.intelligence.common.validation.rule;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RequiredRule Tests")
public class RequiredRuleTest {

    private RequiredRule<String> rule;

    @BeforeEach
    void setUp() {
        rule = new RequiredRule<>();
    }

    @Test
    @DisplayName("Should fail validation when value is null")
    void shouldFailWhenNull() {
        // Act
        boolean isValid = rule.isValid(null);

        // Assert
        assertFalse(isValid, "Null value should fail validation");
    }

    @Test
    @DisplayName("Should fail validation when string is empty")
    void shouldFailWhenEmpty() {
        // Arrange
        String value = "";

        // Act
        boolean isValid = rule.isValid(value);

        // Assert
        assertFalse(isValid, "Empty string should fail validation");
    }

    @Test
    @DisplayName("Should fail validation when string contains only whitespace")
    void shouldFailWhenOnlyWhitespace() {
        // Arrange
        String value = "   ";

        // Act
        boolean isValid = rule.isValid(value);

        // Assert
        assertFalse(isValid, "Whitespace-only string should fail validation");
    }

    @Test
    @DisplayName("Should fail validation when string contains only tabs and newlines")
    void shouldFailWhenOnlyTabsAndNewlines() {
        // Arrange
        String value = "\t\n\r";

        // Act
        boolean isValid = rule.isValid(value);

        // Assert
        assertFalse(isValid, "Tabs and newlines only should fail validation");
    }

    @Test
    @DisplayName("Should pass validation when string contains valid content")
    void shouldPassWhenValid() {
        // Arrange
        String value = "valid content";

        // Act
        boolean isValid = rule.isValid(value);

        // Assert
        assertTrue(isValid, "Valid content should pass validation");
    }

    @Test
    @DisplayName("Should pass validation when string has leading/trailing whitespace but contains content")
    void shouldPassWhenContentWithWhitespace() {
        // Arrange
        String value = "  valid  ";

        // Act
        boolean isValid = rule.isValid(value);

        // Assert
        assertTrue(isValid, "Content with whitespace should pass validation");
    }

    @Test
    @DisplayName("Should pass validation when value is a single character")
    void shouldPassWhenSingleCharacter() {
        // Arrange
        String value = "a";

        // Act
        boolean isValid = rule.isValid(value);

        // Assert
        assertTrue(isValid, "Single character should pass validation");
    }

    @Test
    @DisplayName("Should return appropriate error message")
    void shouldReturnErrorMessage() {
        // Arrange & Act
        String errorMessage = rule.getErrorMessage();

        // Assert
        assertNotNull(errorMessage, "Error message should not be null");
        assertTrue(errorMessage.toLowerCase().contains("required"),
                "Error message should mention 'required'");
    }
}
