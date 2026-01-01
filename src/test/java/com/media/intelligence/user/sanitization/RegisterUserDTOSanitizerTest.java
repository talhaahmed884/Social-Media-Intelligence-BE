package com.media.intelligence.user.sanitization;

import com.media.intelligence.user.dto.RegisterUserDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RegisterUserDTOSanitizer Tests")
public class RegisterUserDTOSanitizerTest {

    private RegisterUserDTOSanitizer sanitizer;

    @BeforeEach
    void setUp() {
        sanitizer = new RegisterUserDTOSanitizer();
    }

    @Test
    @DisplayName("Should trim whitespace from email")
    void shouldTrimEmailWhitespace() {
        // Arrange
        RegisterUserDTO dto = RegisterUserDTO.builder()
                .email("  user@example.com  ")
                .password("Password123!")
                .fullName("John Doe")
                .build();

        // Act
        RegisterUserDTO result = sanitizer.sanitize(dto);

        // Assert
        assertEquals("user@example.com", result.getEmail(),
                "Should trim whitespace from email");
    }

    @Test
    @DisplayName("Should normalize email to lowercase")
    void shouldNormalizeEmailToLowercase() {
        // Arrange
        RegisterUserDTO dto = RegisterUserDTO.builder()
                .email("USER@EXAMPLE.COM")
                .password("Password123!")
                .fullName("John Doe")
                .build();

        // Act
        RegisterUserDTO result = sanitizer.sanitize(dto);

        // Assert
        assertEquals("user@example.com", result.getEmail(),
                "Should normalize email to lowercase");
    }

    @Test
    @DisplayName("Should remove XSS from email")
    void shouldRemoveXssFromEmail() {
        // Arrange
        RegisterUserDTO dto = RegisterUserDTO.builder()
                .email("user<script>alert(1)</script>@example.com")
                .password("Password123!")
                .fullName("John Doe")
                .build();

        // Act
        RegisterUserDTO result = sanitizer.sanitize(dto);

        // Assert
        assertFalse(result.getEmail().contains("<script>"),
                "Should remove XSS from email");
    }

    @Test
    @DisplayName("Should trim whitespace from password")
    void shouldTrimPasswordWhitespace() {
        // Arrange
        RegisterUserDTO dto = RegisterUserDTO.builder()
                .email("user@example.com")
                .password("  Password123!  ")
                .fullName("John Doe")
                .build();

        // Act
        RegisterUserDTO result = sanitizer.sanitize(dto);

        // Assert
        assertEquals("Password123!", result.getPassword(),
                "Should trim whitespace from password");
    }

    @Test
    @DisplayName("Should preserve password special characters")
    void shouldPreservePasswordSpecialChars() {
        // Arrange
        RegisterUserDTO dto = RegisterUserDTO.builder()
                .email("user@example.com")
                .password("P@ssw0rd!#$%")
                .fullName("John Doe")
                .build();

        // Act
        RegisterUserDTO result = sanitizer.sanitize(dto);

        // Assert
        assertEquals("P@ssw0rd!#$%", result.getPassword(),
                "Should preserve password special characters");
    }

    @Test
    @DisplayName("Should trim whitespace from fullName")
    void shouldTrimFullNameWhitespace() {
        // Arrange
        RegisterUserDTO dto = RegisterUserDTO.builder()
                .email("user@example.com")
                .password("Password123!")
                .fullName("  John Doe  ")
                .build();

        // Act
        RegisterUserDTO result = sanitizer.sanitize(dto);

        // Assert
        assertEquals("John Doe", result.getFullName(),
                "Should trim whitespace from fullName");
    }

    @Test
    @DisplayName("Should remove XSS from fullName")
    void shouldRemoveXssFromFullName() {
        // Arrange
        RegisterUserDTO dto = RegisterUserDTO.builder()
                .email("user@example.com")
                .password("Password123!")
                .fullName("John<script>alert(1)</script> Doe")
                .build();

        // Act
        RegisterUserDTO result = sanitizer.sanitize(dto);

        // Assert
        assertFalse(result.getFullName().contains("<script>"),
                "Should remove XSS from fullName");
        assertTrue(result.getFullName().contains("John"),
                "Should preserve safe content in fullName");
    }

    @Test
    @DisplayName("Should handle Gmail dot removal in email")
    void shouldHandleGmailDotRemoval() {
        // Arrange
        RegisterUserDTO dto = RegisterUserDTO.builder()
                .email("first.last@gmail.com")
                .password("Password123!")
                .fullName("John Doe")
                .build();

        // Act
        RegisterUserDTO result = sanitizer.sanitize(dto);

        // Assert
        assertEquals("firstlast@gmail.com", result.getEmail(),
                "Should remove dots from Gmail address");
    }

    @Test
    @DisplayName("Should not modify non-Gmail addresses")
    void shouldNotModifyNonGmailAddresses() {
        // Arrange
        RegisterUserDTO dto = RegisterUserDTO.builder()
                .email("first.last@outlook.com")
                .password("Password123!")
                .fullName("John Doe")
                .build();

        // Act
        RegisterUserDTO result = sanitizer.sanitize(dto);

        // Assert
        assertEquals("first.last@outlook.com", result.getEmail(),
                "Should not remove dots from non-Gmail addresses");
    }

    @Test
    @DisplayName("Should apply all sanitization rules in order")
    void shouldApplyAllRulesInOrder() {
        // Arrange
        RegisterUserDTO dto = RegisterUserDTO.builder()
                .email("  USER@EXAMPLE.COM<script>  ")
                .password("  Password123!  ")
                .fullName("  John<iframe> Doe  ")
                .build();

        // Act
        RegisterUserDTO result = sanitizer.sanitize(dto);

        // Assert
        assertFalse(result.getEmail().contains("<script>"),
                "Should remove XSS from email");
        assertEquals("user@example.com", result.getEmail(),
                "Should trim and normalize email");
        assertEquals("Password123!", result.getPassword(),
                "Should trim password");
        assertFalse(result.getFullName().contains("<iframe>"),
                "Should remove XSS from fullName");
        assertTrue(result.getFullName().contains("John"),
                "Should preserve safe content");
    }

    @Test
    @DisplayName("Should handle null fields gracefully")
    void shouldHandleNullFields() {
        // Arrange
        RegisterUserDTO dto = RegisterUserDTO.builder()
                .email(null)
                .password(null)
                .fullName(null)
                .build();

        // Act
        RegisterUserDTO result = sanitizer.sanitize(dto);

        // Assert
        assertNotNull(result, "Should return DTO even with null fields");
        assertNull(result.getEmail(), "Null email should remain null");
        assertNull(result.getPassword(), "Null password should remain null");
        assertNull(result.getFullName(), "Null fullName should remain null");
    }

    @Test
    @DisplayName("Should return same DTO instance")
    void shouldReturnSameDTOInstance() {
        // Arrange
        RegisterUserDTO dto = RegisterUserDTO.builder()
                .email("user@example.com")
                .password("Password123!")
                .fullName("John Doe")
                .build();

        // Act
        RegisterUserDTO result = sanitizer.sanitize(dto);

        // Assert
        assertSame(dto, result, "Should return same DTO instance (in-place modification)");
    }
}
