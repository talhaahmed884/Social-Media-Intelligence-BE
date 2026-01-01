package com.media.intelligence.user.validation;

import com.media.intelligence.common.validation.core.ValidationResult;
import com.media.intelligence.user.dto.RegisterUserDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("RegisterUserDTOValidator Tests")
public class RegisterUserDTOValidatorTest {

    private RegisterUserDTOValidator validator;

    @BeforeEach
    void setUp() {
        validator = new RegisterUserDTOValidator();
    }

    @Test
    @DisplayName("Should pass validation for valid DTO")
    void shouldPassForValidDTO() {
        // Arrange
        RegisterUserDTO dto = RegisterUserDTO.builder()
                .email("user@example.com")
                .password("SecurePass123!")
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
        RegisterUserDTO dto = RegisterUserDTO.builder()
                .email(null)
                .password("SecurePass123!")
                .fullName("John Doe")
                .build();

        // Act
        ValidationResult result = validator.validate(dto);

        // Assert
        assertTrue(result.hasErrors(), "Should fail when email is null");
        assertTrue(result.getErrors().stream()
                        .anyMatch(e -> e.getField().equals("email")),
                "Should have error for email field");
    }

    @Test
    @DisplayName("Should fail when email is empty")
    void shouldFailWhenEmailIsEmpty() {
        // Arrange
        RegisterUserDTO dto = RegisterUserDTO.builder()
                .email("")
                .password("SecurePass123!")
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
        RegisterUserDTO dto = RegisterUserDTO.builder()
                .email("invalid-email")
                .password("SecurePass123!")
                .fullName("John Doe")
                .build();

        // Act
        ValidationResult result = validator.validate(dto);

        // Assert
        assertTrue(result.hasErrors(), "Should fail for invalid email format");
    }

    @Test
    @DisplayName("Should fail when email exceeds max length")
    void shouldFailWhenEmailTooLong() {
        // Arrange
        String longEmail = "a".repeat(250) + "@test.com"; // > 255 chars
        RegisterUserDTO dto = RegisterUserDTO.builder()
                .email(longEmail)
                .password("SecurePass123!")
                .fullName("John Doe")
                .build();

        // Act
        ValidationResult result = validator.validate(dto);

        // Assert
        assertTrue(result.hasErrors(), "Should fail when email too long");
    }

    @Test
    @DisplayName("Should fail when password is null")
    void shouldFailWhenPasswordIsNull() {
        // Arrange
        RegisterUserDTO dto = RegisterUserDTO.builder()
                .email("user@example.com")
                .password(null)
                .fullName("John Doe")
                .build();

        // Act
        ValidationResult result = validator.validate(dto);

        // Assert
        assertTrue(result.hasErrors(), "Should fail when password is null");
        assertTrue(result.getErrors().stream()
                        .anyMatch(e -> e.getField().equals("password")),
                "Should have error for password field");
    }

    @Test
    @DisplayName("Should fail when password is too weak")
    void shouldFailWhenPasswordTooWeak() {
        // Arrange
        RegisterUserDTO dto = RegisterUserDTO.builder()
                .email("user@example.com")
                .password("weak")
                .fullName("John Doe")
                .build();

        // Act
        ValidationResult result = validator.validate(dto);

        // Assert
        assertTrue(result.hasErrors(), "Should fail for weak password");
    }

    @Test
    @DisplayName("Should fail when password missing uppercase")
    void shouldFailWhenPasswordMissingUppercase() {
        // Arrange
        RegisterUserDTO dto = RegisterUserDTO.builder()
                .email("user@example.com")
                .password("lowercase123!")
                .fullName("John Doe")
                .build();

        // Act
        ValidationResult result = validator.validate(dto);

        // Assert
        assertTrue(result.hasErrors(), "Should fail when password missing uppercase");
    }

    @Test
    @DisplayName("Should fail when password missing special character")
    void shouldFailWhenPasswordMissingSpecialChar() {
        // Arrange
        RegisterUserDTO dto = RegisterUserDTO.builder()
                .email("user@example.com")
                .password("Password123")
                .fullName("John Doe")
                .build();

        // Act
        ValidationResult result = validator.validate(dto);

        // Assert
        assertTrue(result.hasErrors(), "Should fail when password missing special char");
    }

    @Test
    @DisplayName("Should fail when fullName is null")
    void shouldFailWhenFullNameIsNull() {
        // Arrange
        RegisterUserDTO dto = RegisterUserDTO.builder()
                .email("user@example.com")
                .password("SecurePass123!")
                .fullName(null)
                .build();

        // Act
        ValidationResult result = validator.validate(dto);

        // Assert
        assertTrue(result.hasErrors(), "Should fail when fullName is null");
        assertTrue(result.getErrors().stream()
                        .anyMatch(e -> e.getField().equals("fullName")),
                "Should have error for fullName field");
    }

    @Test
    @DisplayName("Should fail when fullName is too short")
    void shouldFailWhenFullNameTooShort() {
        // Arrange
        RegisterUserDTO dto = RegisterUserDTO.builder()
                .email("user@example.com")
                .password("SecurePass123!")
                .fullName("A")
                .build();

        // Act
        ValidationResult result = validator.validate(dto);

        // Assert
        assertTrue(result.hasErrors(), "Should fail when fullName too short");
    }

    @Test
    @DisplayName("Should fail when fullName exceeds max length")
    void shouldFailWhenFullNameTooLong() {
        // Arrange
        String longName = "A".repeat(256); // > 255 chars
        RegisterUserDTO dto = RegisterUserDTO.builder()
                .email("user@example.com")
                .password("SecurePass123!")
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
        RegisterUserDTO dto = RegisterUserDTO.builder()
                .email("invalid")
                .password("weak")
                .fullName("A")
                .build();

        // Act
        ValidationResult result = validator.validate(dto);

        // Assert
        assertTrue(result.hasErrors(), "Should have errors");
        assertTrue(result.getErrors().size() > 1, "Should accumulate multiple errors");
    }

    @Test
    @DisplayName("Should pass for minimum valid fullName length")
    void shouldPassForMinimumFullNameLength() {
        // Arrange
        RegisterUserDTO dto = RegisterUserDTO.builder()
                .email("user@example.com")
                .password("SecurePass123!")
                .fullName("AB") // exactly 2 chars
                .build();

        // Act
        ValidationResult result = validator.validate(dto);

        // Assert
        assertFalse(result.hasErrors(), "Should pass for 2-character fullName");
    }

    @Test
    @DisplayName("Should pass for maximum valid email length")
    void shouldPassForMaximumEmailLength() {
        // Arrange
        String maxEmail = "a".repeat(240) + "@example.com"; // exactly 255 chars
        RegisterUserDTO dto = RegisterUserDTO.builder()
                .email(maxEmail)
                .password("SecurePass123!")
                .fullName("John Doe")
                .build();

        // Act
        ValidationResult result = validator.validate(dto);

        // Assert
        assertFalse(result.hasErrors(), "Should pass for 255-character email");
    }
}
