package com.media.intelligence.user.sanitization;

import com.media.intelligence.user.dto.ChangePasswordDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ChangePasswordDTOSanitizer Tests")
public class ChangePasswordDTOSanitizerTest {

    private ChangePasswordDTOSanitizer sanitizer;

    @BeforeEach
    void setUp() {
        sanitizer = new ChangePasswordDTOSanitizer();
    }

    @Test
    @DisplayName("Should trim whitespace from currentPassword")
    void shouldTrimCurrentPasswordWhitespace() {
        // Arrange
        ChangePasswordDTO dto = ChangePasswordDTO.builder()
                .currentPassword("  OldPass123!  ")
                .newPassword("NewPass123!")
                .build();

        // Act
        ChangePasswordDTO result = sanitizer.sanitize(dto);

        // Assert
        assertEquals("OldPass123!", result.getCurrentPassword());
    }

    @Test
    @DisplayName("Should trim whitespace from newPassword")
    void shouldTrimNewPasswordWhitespace() {
        // Arrange
        ChangePasswordDTO dto = ChangePasswordDTO.builder()
                .currentPassword("OldPass123!")
                .newPassword("  NewPass123!  ")
                .build();

        // Act
        ChangePasswordDTO result = sanitizer.sanitize(dto);

        // Assert
        assertEquals("NewPass123!", result.getNewPassword());
    }

    @Test
    @DisplayName("Should preserve special characters in currentPassword")
    void shouldPreserveSpecialCharsInCurrentPassword() {
        // Arrange
        ChangePasswordDTO dto = ChangePasswordDTO.builder()
                .currentPassword("P@ssw0rd!#$%")
                .newPassword("NewPass123!")
                .build();

        // Act
        ChangePasswordDTO result = sanitizer.sanitize(dto);

        // Assert
        assertEquals("P@ssw0rd!#$%", result.getCurrentPassword());
    }

    @Test
    @DisplayName("Should preserve special characters in newPassword")
    void shouldPreserveSpecialCharsInNewPassword() {
        // Arrange
        ChangePasswordDTO dto = ChangePasswordDTO.builder()
                .currentPassword("OldPass123!")
                .newPassword("N3w!@#P@ssw0rd$%^")
                .build();

        // Act
        ChangePasswordDTO result = sanitizer.sanitize(dto);

        // Assert
        assertEquals("N3w!@#P@ssw0rd$%^", result.getNewPassword());
    }

    @Test
    @DisplayName("Should handle tabs and newlines")
    void shouldHandleTabsAndNewlines() {
        // Arrange
        ChangePasswordDTO dto = ChangePasswordDTO.builder()
                .currentPassword("\tOldPass123!\n")
                .newPassword("\nNewPass123!\t")
                .build();

        // Act
        ChangePasswordDTO result = sanitizer.sanitize(dto);

        // Assert
        assertEquals("OldPass123!", result.getCurrentPassword());
        assertEquals("NewPass123!", result.getNewPassword());
    }

    @Test
    @DisplayName("Should handle passwords with no whitespace")
    void shouldHandleNoWhitespace() {
        // Arrange
        ChangePasswordDTO dto = ChangePasswordDTO.builder()
                .currentPassword("OldPass123!")
                .newPassword("NewPass123!")
                .build();

        // Act
        ChangePasswordDTO result = sanitizer.sanitize(dto);

        // Assert
        assertEquals("OldPass123!", result.getCurrentPassword());
        assertEquals("NewPass123!", result.getNewPassword());
    }

    @Test
    @DisplayName("Should handle null fields")
    void shouldHandleNullFields() {
        // Arrange
        ChangePasswordDTO dto = ChangePasswordDTO.builder()
                .currentPassword(null)
                .newPassword(null)
                .build();

        // Act
        ChangePasswordDTO result = sanitizer.sanitize(dto);

        // Assert
        assertNull(result.getCurrentPassword());
        assertNull(result.getNewPassword());
    }

    @Test
    @DisplayName("Should handle empty passwords")
    void shouldHandleEmptyPasswords() {
        // Arrange
        ChangePasswordDTO dto = ChangePasswordDTO.builder()
                .currentPassword("")
                .newPassword("")
                .build();

        // Act
        ChangePasswordDTO result = sanitizer.sanitize(dto);

        // Assert
        assertEquals("", result.getCurrentPassword());
        assertEquals("", result.getNewPassword());
    }

    @Test
    @DisplayName("Should handle whitespace-only passwords")
    void shouldHandleWhitespaceOnlyPasswords() {
        // Arrange
        ChangePasswordDTO dto = ChangePasswordDTO.builder()
                .currentPassword("   ")
                .newPassword("   ")
                .build();

        // Act
        ChangePasswordDTO result = sanitizer.sanitize(dto);

        // Assert
        assertEquals("", result.getCurrentPassword());
        assertEquals("", result.getNewPassword());
    }

    @Test
    @DisplayName("Should return same DTO instance")
    void shouldReturnSameDTOInstance() {
        // Arrange
        ChangePasswordDTO dto = ChangePasswordDTO.builder()
                .currentPassword("OldPass123!")
                .newPassword("NewPass123!")
                .build();

        // Act
        ChangePasswordDTO result = sanitizer.sanitize(dto);

        // Assert
        assertSame(dto, result);
    }
}
