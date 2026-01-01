package com.media.intelligence.user.validation;

import com.media.intelligence.common.validation.core.ValidationResult;
import com.media.intelligence.user.dto.UpdateUserProfileDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("UpdateUserProfileDTOValidator Tests")
public class UpdateUserProfileDTOValidatorTest {

    private UpdateUserProfileDTOValidator validator;

    @BeforeEach
    void setUp() {
        validator = new UpdateUserProfileDTOValidator();
    }

    @Test
    @DisplayName("Should pass validation for valid DTO")
    void shouldPassForValidDTO() {
        // Arrange
        UpdateUserProfileDTO dto = UpdateUserProfileDTO.builder()
                .email("user@example.com")
                .fullName("John Doe")
                .build();

        // Act
        ValidationResult result = validator.validate(dto);

        // Assert
        assertFalse(result.hasErrors(), "Valid DTO should pass validation");
    }

    @Test
    @DisplayName("Should fail when email is null")
    void shouldFailWhenEmailIsNull() {
        // Arrange
        UpdateUserProfileDTO dto = UpdateUserProfileDTO.builder()
                .email(null)
                .fullName("John Doe")
                .build();

        // Act
        ValidationResult result = validator.validate(dto);

        // Assert
        assertTrue(result.hasErrors(), "Should fail when email is null");
    }

    @Test
    @DisplayName("Should fail when email is empty")
    void shouldFailWhenEmailIsEmpty() {
        // Arrange
        UpdateUserProfileDTO dto = UpdateUserProfileDTO.builder()
                .email("")
                .fullName("John Doe")
                .build();

        // Act
        ValidationResult result = validator.validate(dto);

        // Assert
        assertTrue(result.hasErrors(), "Should fail when email is empty");
    }

    @Test
    @DisplayName("Should fail when email format is invalid")
    void shouldFailWhenEmailFormatInvalid() {
        // Arrange
        UpdateUserProfileDTO dto = UpdateUserProfileDTO.builder()
                .email("not-an-email")
                .fullName("John Doe")
                .build();

        // Act
        ValidationResult result = validator.validate(dto);

        // Assert
        assertTrue(result.hasErrors(), "Should fail for invalid email format");
    }

    @Test
    @DisplayName("Should fail when fullName is null")
    void shouldFailWhenFullNameIsNull() {
        // Arrange
        UpdateUserProfileDTO dto = UpdateUserProfileDTO.builder()
                .email("user@example.com")
                .fullName(null)
                .build();

        // Act
        ValidationResult result = validator.validate(dto);

        // Assert
        assertTrue(result.hasErrors(), "Should fail when fullName is null");
    }

    @Test
    @DisplayName("Should fail when fullName is too short")
    void shouldFailWhenFullNameTooShort() {
        // Arrange
        UpdateUserProfileDTO dto = UpdateUserProfileDTO.builder()
                .email("user@example.com")
                .fullName("A")
                .build();

        // Act
        ValidationResult result = validator.validate(dto);

        // Assert
        assertTrue(result.hasErrors(), "Should fail when fullName too short");
    }

    @Test
    @DisplayName("Should fail when fullName is too long")
    void shouldFailWhenFullNameTooLong() {
        // Arrange
        String longName = "A".repeat(256);
        UpdateUserProfileDTO dto = UpdateUserProfileDTO.builder()
                .email("user@example.com")
                .fullName(longName)
                .build();

        // Act
        ValidationResult result = validator.validate(dto);

        // Assert
        assertTrue(result.hasErrors(), "Should fail when fullName too long");
    }

    @Test
    @DisplayName("Should accumulate multiple errors")
    void shouldAccumulateMultipleErrors() {
        // Arrange
        UpdateUserProfileDTO dto = UpdateUserProfileDTO.builder()
                .email("invalid")
                .fullName("A")
                .build();

        // Act
        ValidationResult result = validator.validate(dto);

        // Assert
        assertTrue(result.hasErrors(), "Should have errors");
        assertTrue(result.getErrors().size() > 1, "Should accumulate multiple errors");
    }

    @Test
    @DisplayName("Should pass for minimum valid fullName")
    void shouldPassForMinimumFullName() {
        // Arrange
        UpdateUserProfileDTO dto = UpdateUserProfileDTO.builder()
                .email("user@example.com")
                .fullName("AB")
                .build();

        // Act
        ValidationResult result = validator.validate(dto);

        // Assert
        assertFalse(result.hasErrors(), "Should pass for 2-character fullName");
    }
}
