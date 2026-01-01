package com.media.intelligence.user.validation;

import com.media.intelligence.common.validation.core.ValidationResult;
import com.media.intelligence.user.dto.FindUserByEmailDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("FindUserByEmailDTOValidator Tests")
public class FindUserByEmailDTOValidatorTest {

    private FindUserByEmailDTOValidator validator;

    @BeforeEach
    void setUp() {
        validator = new FindUserByEmailDTOValidator();
    }

    @Test
    @DisplayName("Should pass validation for valid DTO")
    void shouldPassForValidDTO() {
        // Arrange
        FindUserByEmailDTO dto = FindUserByEmailDTO.builder()
                .email("user@example.com")
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
        FindUserByEmailDTO dto = FindUserByEmailDTO.builder()
                .email(null)
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
        FindUserByEmailDTO dto = FindUserByEmailDTO.builder()
                .email("")
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
        FindUserByEmailDTO dto = FindUserByEmailDTO.builder()
                .email("not-an-email")
                .build();

        // Act
        ValidationResult result = validator.validate(dto);

        // Assert
        assertTrue(result.hasErrors(), "Should fail for invalid email format");
    }

    @Test
    @DisplayName("Should pass for various valid email formats")
    void shouldPassForValidEmailFormats() {
        // Arrange
        String[] validEmails = {
                "user@example.com",
                "john.doe@company.co.uk",
                "test+tag@gmail.com",
                "user_name@domain.com"
        };

        // Act & Assert
        for (String email : validEmails) {
            FindUserByEmailDTO dto = FindUserByEmailDTO.builder()
                    .email(email)
                    .build();
            ValidationResult result = validator.validate(dto);
            assertFalse(result.hasErrors(), "Should pass for valid email: " + email);
        }
    }

    @Test
    @DisplayName("Should fail when email exceeds max length")
    void shouldFailWhenEmailTooLong() {
        // Arrange
        String longEmail = "a".repeat(250) + "@test.com";
        FindUserByEmailDTO dto = FindUserByEmailDTO.builder()
                .email(longEmail)
                .build();

        // Act
        ValidationResult result = validator.validate(dto);

        // Assert
        assertTrue(result.hasErrors(), "Should fail when email too long");
    }
}
