package com.media.intelligence.common.sanitization.rule;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("EmailNormalizationRule Tests")
public class EmailNormalizationRuleTest {

    private EmailNormalizationRule rule;

    @BeforeEach
    void setUp() {
        rule = new EmailNormalizationRule();
    }

    @Test
    @DisplayName("Should convert email to lowercase")
    void shouldConvertToLowercase() {
        // Arrange
        String input = "USER@EXAMPLE.COM";

        // Act
        String result = rule.sanitize(input);

        // Assert
        assertEquals("user@example.com", result, "Should convert to lowercase");
    }

    @Test
    @DisplayName("Should handle mixed case email")
    void shouldHandleMixedCase() {
        // Arrange
        String input = "UsEr@ExAmPlE.CoM";

        // Act
        String result = rule.sanitize(input);

        // Assert
        assertEquals("user@example.com", result, "Should normalize mixed case");
    }

    @Test
    @DisplayName("Should handle already lowercase email")
    void shouldHandleAlreadyLowercase() {
        // Arrange
        String input = "user@example.com";

        // Act
        String result = rule.sanitize(input);

        // Assert
        assertEquals("user@example.com", result, "Should not change already lowercase email");
    }

    @Test
    @DisplayName("Should handle Gmail dot removal")
    void shouldHandleGmailDotRemoval() {
        // Arrange
        String input = "first.last@gmail.com";

        // Act
        String result = rule.sanitize(input);

        // Assert
        assertEquals("firstlast@gmail.com", result,
                "Should remove dots from Gmail address local part");
    }

    @Test
    @DisplayName("Should handle Gmail with multiple dots")
    void shouldHandleMultipleDots() {
        // Arrange
        String input = "f.i.r.s.t.l.a.s.t@gmail.com";

        // Act
        String result = rule.sanitize(input);

        // Assert
        assertEquals("firstlast@gmail.com", result,
                "Should remove all dots from Gmail address");
    }

    @Test
    @DisplayName("Should handle Gmail plus addressing")
    void shouldHandleGmailPlusAddressing() {
        // Arrange
        String input = "user+tag@gmail.com";

        // Act
        String result = rule.sanitize(input);

        // Assert
        assertTrue(result.equals("user@gmail.com") || result.equals("user+tag@gmail.com"),
                "Should handle plus addressing (implementation may vary)");
    }

    @Test
    @DisplayName("Should not remove dots from non-Gmail addresses")
    void shouldNotRemoveDotsFromNonGmail() {
        // Arrange
        String input = "first.last@outlook.com";

        // Act
        String result = rule.sanitize(input);

        // Assert
        assertEquals("first.last@outlook.com", result,
                "Should preserve dots in non-Gmail addresses");
    }

    @Test
    @DisplayName("Should handle null input")
    void shouldHandleNullInput() {
        // Act
        String result = rule.sanitize(null);

        // Assert
        assertNull(result, "Should return null for null input");
    }

    @Test
    @DisplayName("Should handle empty input")
    void shouldHandleEmptyInput() {
        // Arrange
        String input = "";

        // Act
        String result = rule.sanitize(input);

        // Assert
        assertEquals("", result, "Should return empty string for empty input");
    }

    @Test
    @DisplayName("Should handle Gmail with uppercase")
    void shouldHandleGmailWithUppercase() {
        // Arrange
        String input = "First.Last@GMAIL.COM";

        // Act
        String result = rule.sanitize(input);

        // Assert
        assertEquals("firstlast@gmail.com", result,
                "Should lowercase and remove dots for Gmail");
    }

    @Test
    @DisplayName("Should preserve domain for non-Gmail")
    void shouldPreserveDomain() {
        // Arrange
        String input = "user@company.co.uk";

        // Act
        String result = rule.sanitize(input);

        // Assert
        assertEquals("user@company.co.uk", result,
                "Should preserve domain structure");
    }

    @Test
    @DisplayName("Should handle email with numbers")
    void shouldHandleEmailWithNumbers() {
        // Arrange
        String input = "User123@Example.COM";

        // Act
        String result = rule.sanitize(input);

        // Assert
        assertEquals("user123@example.com", result,
                "Should lowercase email with numbers");
    }

    @Test
    @DisplayName("Should handle email with underscores")
    void shouldHandleEmailWithUnderscores() {
        // Arrange
        String input = "First_Last@EXAMPLE.COM";

        // Act
        String result = rule.sanitize(input);

        // Assert
        assertEquals("first_last@example.com", result,
                "Should preserve underscores and lowercase");
    }

    @Test
    @DisplayName("Should handle email with hyphens in domain")
    void shouldHandleHyphensInDomain() {
        // Arrange
        String input = "user@test-domain.COM";

        // Act
        String result = rule.sanitize(input);

        // Assert
        assertEquals("user@test-domain.com", result,
                "Should preserve hyphens in domain and lowercase");
    }

    @Test
    @DisplayName("Should handle invalid email format gracefully")
    void shouldHandleInvalidEmailFormat() {
        // Arrange
        String input = "notanemail";

        // Act
        String result = rule.sanitize(input);

        // Assert
        assertEquals("notanemail", result,
                "Should return lowercased input even if not valid email format");
    }
}
