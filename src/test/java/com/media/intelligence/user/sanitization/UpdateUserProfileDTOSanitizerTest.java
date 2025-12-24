package com.media.intelligence.user.sanitization;

import com.media.intelligence.user.dto.UpdateUserProfileDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("UpdateUserProfileDTOSanitizer Tests")
public class UpdateUserProfileDTOSanitizerTest {

    private UpdateUserProfileDTOSanitizer sanitizer;

    @BeforeEach
    void setUp() {
        sanitizer = new UpdateUserProfileDTOSanitizer();
    }

    @Test
    @DisplayName("Should trim whitespace from email")
    void shouldTrimEmailWhitespace() {
        // Arrange
        UpdateUserProfileDTO dto = UpdateUserProfileDTO.builder()
                .email("  user@example.com  ")
                .fullName("John Doe")
                .build();

        // Act
        UpdateUserProfileDTO result = sanitizer.sanitize(dto);

        // Assert
        assertEquals("user@example.com", result.getEmail());
    }

    @Test
    @DisplayName("Should normalize email to lowercase")
    void shouldNormalizeEmailToLowercase() {
        // Arrange
        UpdateUserProfileDTO dto = UpdateUserProfileDTO.builder()
                .email("USER@EXAMPLE.COM")
                .fullName("John Doe")
                .build();

        // Act
        UpdateUserProfileDTO result = sanitizer.sanitize(dto);

        // Assert
        assertEquals("user@example.com", result.getEmail());
    }

    @Test
    @DisplayName("Should remove XSS from email")
    void shouldRemoveXssFromEmail() {
        // Arrange
        UpdateUserProfileDTO dto = UpdateUserProfileDTO.builder()
                .email("user<script>@example.com")
                .fullName("John Doe")
                .build();

        // Act
        UpdateUserProfileDTO result = sanitizer.sanitize(dto);

        // Assert
        assertFalse(result.getEmail().contains("<script>"));
    }

    @Test
    @DisplayName("Should trim whitespace from fullName")
    void shouldTrimFullNameWhitespace() {
        // Arrange
        UpdateUserProfileDTO dto = UpdateUserProfileDTO.builder()
                .email("user@example.com")
                .fullName("  John Doe  ")
                .build();

        // Act
        UpdateUserProfileDTO result = sanitizer.sanitize(dto);

        // Assert
        assertEquals("John Doe", result.getFullName());
    }

    @Test
    @DisplayName("Should remove XSS from fullName")
    void shouldRemoveXssFromFullName() {
        // Arrange
        UpdateUserProfileDTO dto = UpdateUserProfileDTO.builder()
                .email("user@example.com")
                .fullName("John<script>alert(1)</script> Doe")
                .build();

        // Act
        UpdateUserProfileDTO result = sanitizer.sanitize(dto);

        // Assert
        assertFalse(result.getFullName().contains("<script>"));
        assertTrue(result.getFullName().contains("John"));
    }

    @Test
    @DisplayName("Should handle Gmail dot removal")
    void shouldHandleGmailDotRemoval() {
        // Arrange
        UpdateUserProfileDTO dto = UpdateUserProfileDTO.builder()
                .email("first.last@gmail.com")
                .fullName("John Doe")
                .build();

        // Act
        UpdateUserProfileDTO result = sanitizer.sanitize(dto);

        // Assert
        assertEquals("firstlast@gmail.com", result.getEmail());
    }

    @Test
    @DisplayName("Should handle null fields")
    void shouldHandleNullFields() {
        // Arrange
        UpdateUserProfileDTO dto = UpdateUserProfileDTO.builder()
                .email(null)
                .fullName(null)
                .build();

        // Act
        UpdateUserProfileDTO result = sanitizer.sanitize(dto);

        // Assert
        assertNull(result.getEmail());
        assertNull(result.getFullName());
    }

    @Test
    @DisplayName("Should return same DTO instance")
    void shouldReturnSameDTOInstance() {
        // Arrange
        UpdateUserProfileDTO dto = UpdateUserProfileDTO.builder()
                .email("user@example.com")
                .fullName("John Doe")
                .build();

        // Act
        UpdateUserProfileDTO result = sanitizer.sanitize(dto);

        // Assert
        assertSame(dto, result);
    }
}
