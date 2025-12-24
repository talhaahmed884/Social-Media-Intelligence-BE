package com.media.intelligence.user.validation;

import com.media.intelligence.common.validation.core.ValidationResult;
import com.media.intelligence.user.dto.ChangePasswordDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("ChangePasswordDTOValidator Tests")
public class ChangePasswordDTOValidatorTest {

    private ChangePasswordDTOValidator validator;

    @BeforeEach
    void setUp() {
        validator = new ChangePasswordDTOValidator();
    }

    @Test
    @DisplayName("Should pass validation for valid DTO")
    void shouldPassForValidDTO() {
        // Arrange
        ChangePasswordDTO dto = ChangePasswordDTO.builder()
                .currentPassword("OldPass123!")
                .newPassword("NewSecurePass123!")
                .build();

        // Act
        ValidationResult result = validator.validate(dto);

        // Assert
        assertFalse(result.hasErrors(), "Valid DTO should pass validation");
    }

    @Test
    @DisplayName("Should fail when currentPassword is null")
    void shouldFailWhenCurrentPasswordIsNull() {
        // Arrange
        ChangePasswordDTO dto = ChangePasswordDTO.builder()
                .currentPassword(null)
                .newPassword("NewSecurePass123!")
                .build();

        // Act
        ValidationResult result = validator.validate(dto);

        // Assert
        assertTrue(result.hasErrors(), "Should fail when currentPassword is null");
        assertTrue(result.getErrors().stream()
                        .anyMatch(e -> e.getField().equals("currentPassword")),
                "Should have error for currentPassword field");
    }

    @Test
    @DisplayName("Should fail when currentPassword is empty")
    void shouldFailWhenCurrentPasswordIsEmpty() {
        // Arrange
        ChangePasswordDTO dto = ChangePasswordDTO.builder()
                .currentPassword("")
                .newPassword("NewSecurePass123!")
                .build();

        // Act
        ValidationResult result = validator.validate(dto);

        // Assert
        assertTrue(result.hasErrors(), "Should fail when currentPassword is empty");
    }

    @Test
    @DisplayName("Should fail when newPassword is null")
    void shouldFailWhenNewPasswordIsNull() {
        // Arrange
        ChangePasswordDTO dto = ChangePasswordDTO.builder()
                .currentPassword("OldPass123!")
                .newPassword(null)
                .build();

        // Act
        ValidationResult result = validator.validate(dto);

        // Assert
        assertTrue(result.hasErrors(), "Should fail when newPassword is null");
        assertTrue(result.getErrors().stream()
                        .anyMatch(e -> e.getField().equals("newPassword")),
                "Should have error for newPassword field");
    }

    @Test
    @DisplayName("Should fail when newPassword is too weak")
    void shouldFailWhenNewPasswordTooWeak() {
        // Arrange
        ChangePasswordDTO dto = ChangePasswordDTO.builder()
                .currentPassword("OldPass123!")
                .newPassword("weak")
                .build();

        // Act
        ValidationResult result = validator.validate(dto);

        // Assert
        assertTrue(result.hasErrors(), "Should fail for weak newPassword");
    }

    @Test
    @DisplayName("Should fail when newPassword missing uppercase")
    void shouldFailWhenNewPasswordMissingUppercase() {
        // Arrange
        ChangePasswordDTO dto = ChangePasswordDTO.builder()
                .currentPassword("OldPass123!")
                .newPassword("lowercase123!")
                .build();

        // Act
        ValidationResult result = validator.validate(dto);

        // Assert
        assertTrue(result.hasErrors(), "Should fail when newPassword missing uppercase");
    }

    @Test
    @DisplayName("Should fail when newPassword missing lowercase")
    void shouldFailWhenNewPasswordMissingLowercase() {
        // Arrange
        ChangePasswordDTO dto = ChangePasswordDTO.builder()
                .currentPassword("OldPass123!")
                .newPassword("UPPERCASE123!")
                .build();

        // Act
        ValidationResult result = validator.validate(dto);

        // Assert
        assertTrue(result.hasErrors(), "Should fail when newPassword missing lowercase");
    }

    @Test
    @DisplayName("Should fail when newPassword missing digit")
    void shouldFailWhenNewPasswordMissingDigit() {
        // Arrange
        ChangePasswordDTO dto = ChangePasswordDTO.builder()
                .currentPassword("OldPass123!")
                .newPassword("Password!")
                .build();

        // Act
        ValidationResult result = validator.validate(dto);

        // Assert
        assertTrue(result.hasErrors(), "Should fail when newPassword missing digit");
    }

    @Test
    @DisplayName("Should fail when newPassword missing special character")
    void shouldFailWhenNewPasswordMissingSpecialChar() {
        // Arrange
        ChangePasswordDTO dto = ChangePasswordDTO.builder()
                .currentPassword("OldPass123!")
                .newPassword("Password123")
                .build();

        // Act
        ValidationResult result = validator.validate(dto);

        // Assert
        assertTrue(result.hasErrors(), "Should fail when newPassword missing special char");
    }

    @Test
    @DisplayName("Should fail when newPassword is too short")
    void shouldFailWhenNewPasswordTooShort() {
        // Arrange
        ChangePasswordDTO dto = ChangePasswordDTO.builder()
                .currentPassword("OldPass123!")
                .newPassword("Pass1!")
                .build();

        // Act
        ValidationResult result = validator.validate(dto);

        // Assert
        assertTrue(result.hasErrors(), "Should fail when newPassword too short");
    }

    @Test
    @DisplayName("Should accumulate multiple errors")
    void shouldAccumulateMultipleErrors() {
        // Arrange
        ChangePasswordDTO dto = ChangePasswordDTO.builder()
                .currentPassword("")
                .newPassword("weak")
                .build();

        // Act
        ValidationResult result = validator.validate(dto);

        // Assert
        assertTrue(result.hasErrors(), "Should have errors");
        assertTrue(result.getErrors().size() > 1, "Should accumulate multiple errors");
    }

    @Test
    @DisplayName("Should pass for minimum valid newPassword")
    void shouldPassForMinimumValidNewPassword() {
        // Arrange
        ChangePasswordDTO dto = ChangePasswordDTO.builder()
                .currentPassword("Anything!2")
                .newPassword("SecPass1!") // 9 chars, all requirements
                .build();

        // Act
        ValidationResult result = validator.validate(dto);

        // Assert
        assertFalse(result.hasErrors(), "Should pass for minimum valid newPassword");
    }
}
