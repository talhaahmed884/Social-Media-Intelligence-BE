package com.media.intelligence.common.sanitization.rule;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("XssSanitizationRule Tests")
public class XssSanitizationRuleTest {

    private XssSanitizationRule rule;

    @BeforeEach
    void setUp() {
        rule = new XssSanitizationRule();
    }

    @Test
    @DisplayName("Should remove script tags")
    void shouldRemoveScriptTags() {
        // Arrange
        String input = "<script>alert('XSS')</script>hello";

        // Act
        String result = rule.sanitize(input);

        // Assert
        assertFalse(result.contains("<script>"), "Should remove script opening tag");
        assertFalse(result.contains("</script>"), "Should remove script closing tag");
        assertTrue(result.contains("hello"), "Should preserve safe content");
    }

    @Test
    @DisplayName("Should remove script tags case-insensitively")
    void shouldRemoveScriptTagsCaseInsensitive() {
        // Arrange
        String input = "<SCRIPT>alert('XSS')</SCRIPT>hello";

        // Act
        String result = rule.sanitize(input);

        // Assert
        assertFalse(result.toLowerCase().contains("<script>"),
                "Should remove script tags regardless of case");
        assertTrue(result.contains("hello"), "Should preserve safe content");
    }

    @Test
    @DisplayName("Should remove iframe tags")
    void shouldRemoveIframeTags() {
        // Arrange
        String input = "<iframe src='malicious.com'></iframe>hello";

        // Act
        String result = rule.sanitize(input);

        // Assert
        assertFalse(result.contains("<iframe"), "Should remove iframe opening tag");
        assertFalse(result.contains("</iframe>"), "Should remove iframe closing tag");
        assertTrue(result.contains("hello"), "Should preserve safe content");
    }

    @Test
    @DisplayName("Should remove onclick attributes")
    void shouldRemoveOnClickAttributes() {
        // Arrange
        String input = "<div onclick='alert(1)'>Click me</div>";

        // Act
        String result = rule.sanitize(input);

        // Assert
        assertFalse(result.toLowerCase().contains("onclick"),
                "Should remove onclick attribute");
    }

    @Test
    @DisplayName("Should remove onerror attributes")
    void shouldRemoveOnErrorAttributes() {
        // Arrange
        String input = "<img src=x onerror='alert(1)' />";

        // Act
        String result = rule.sanitize(input);

        // Assert
        assertFalse(result.toLowerCase().contains("onerror"),
                "Should remove onerror attribute");
    }

    @Test
    @DisplayName("Should remove javascript: protocol")
    void shouldRemoveJavaScriptProtocol() {
        // Arrange
        String input = "<a href='javascript:alert(1)'>Click</a>";

        // Act
        String result = rule.sanitize(input);

        // Assert
        assertFalse(result.toLowerCase().contains("javascript:"),
                "Should remove javascript: protocol");
    }

    @Test
    @DisplayName("Should handle multiple XSS attempts in one string")
    void shouldHandleMultipleXssAttempts() {
        // Arrange
        String input = "<script>alert(1)</script><iframe></iframe><div onclick='hack()'>text</div>";

        // Act
        String result = rule.sanitize(input);

        // Assert
        assertFalse(result.contains("<script>"), "Should remove all script tags");
        assertFalse(result.contains("<iframe>"), "Should remove all iframe tags");
        assertFalse(result.toLowerCase().contains("onclick"), "Should remove all onclick attributes");
    }

    @Test
    @DisplayName("Should preserve safe HTML content")
    void shouldPreserveSafeContent() {
        // Arrange
        String input = "Hello World! This is safe text.";

        // Act
        String result = rule.sanitize(input);

        // Assert
        assertEquals(input, result, "Should preserve completely safe text");
    }

    @Test
    @DisplayName("Should handle null input")
    void shouldHandleNullInput() {
        // Arrange
        String input = null;

        // Act
        String result = rule.sanitize(input);

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
    @DisplayName("Should remove style tags")
    void shouldRemoveStyleTags() {
        // Arrange
        String input = "<style>body{background:red;}</style>hello";

        // Act
        String result = rule.sanitize(input);

        // Assert
        assertFalse(result.contains("<style>"), "Should remove style tags");
        assertTrue(result.contains("hello"), "Should preserve safe content");
    }

    @Test
    @DisplayName("Should remove object tags")
    void shouldRemoveObjectTags() {
        // Arrange
        String input = "<object data='malicious.swf'></object>hello";

        // Act
        String result = rule.sanitize(input);

        // Assert
        assertFalse(result.contains("<object"), "Should remove object tags");
        assertTrue(result.contains("hello"), "Should preserve safe content");
    }

    @Test
    @DisplayName("Should remove embed tags")
    void shouldRemoveEmbedTags() {
        // Arrange
        String input = "<embed src='malicious.swf'>hello";

        // Act
        String result = rule.sanitize(input);

        // Assert
        assertFalse(result.contains("<embed"), "Should remove embed tags");
        assertTrue(result.contains("hello"), "Should preserve safe content");
    }

    @Test
    @DisplayName("Should handle nested tags")
    void shouldHandleNestedTags() {
        // Arrange
        String input = "<div><script>alert(1)</script></div>hello";

        // Act
        String result = rule.sanitize(input);

        // Assert
        assertFalse(result.contains("<script>"), "Should remove nested script tags");
        assertTrue(result.contains("hello"), "Should preserve safe content");
    }

    @Test
    @DisplayName("Should sanitize complex XSS payload")
    void shouldSanitizeComplexPayload() {
        // Arrange
        String input = "Hello <script>alert('XSS')</script><img src=x onerror='alert(1)'>" +
                "<iframe src='javascript:alert(1)'></iframe> World!";

        // Act
        String result = rule.sanitize(input);

        // Assert
        assertTrue(result.contains("Hello"), "Should preserve 'Hello'");
        assertTrue(result.contains("World!"), "Should preserve 'World!'");
        assertFalse(result.contains("<script>"), "Should remove script tags");
        assertFalse(result.toLowerCase().contains("onerror"), "Should remove onerror");
        assertFalse(result.contains("<iframe"), "Should remove iframe tags");
    }
}
