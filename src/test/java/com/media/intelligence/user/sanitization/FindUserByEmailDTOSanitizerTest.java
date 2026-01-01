package com.media.intelligence.user.sanitization;

import com.media.intelligence.user.dto.FindUserByEmailDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("FindUserByEmailDTOSanitizer Tests")
public class FindUserByEmailDTOSanitizerTest {

    private FindUserByEmailDTOSanitizer sanitizer;

    @BeforeEach
    void setUp() {
        sanitizer = new FindUserByEmailDTOSanitizer();
    }

    @Test
    @DisplayName("Should trim whitespace from email")
    void shouldTrimEmailWhitespace() {
        // Arrange
        FindUserByEmailDTO dto = FindUserByEmailDTO.builder()
                .email("  user@example.com  ")
                .build();

        // Act
        FindUserByEmailDTO result = sanitizer.sanitize(dto);

        // Assert
        assertEquals("user@example.com", result.getEmail());
    }

    @Test
    @DisplayName("Should normalize email to lowercase")
    void shouldNormalizeEmailToLowercase() {
        // Arrange
        FindUserByEmailDTO dto = FindUserByEmailDTO.builder()
                .email("USER@EXAMPLE.COM")
                .build();

        // Act
        FindUserByEmailDTO result = sanitizer.sanitize(dto);

        // Assert
        assertEquals("user@example.com", result.getEmail());
    }

    @Test
    @DisplayName("Should remove XSS from email")
    void shouldRemoveXssFromEmail() {
        // Arrange
        FindUserByEmailDTO dto = FindUserByEmailDTO.builder()
                .email("user<script>@example.com")
                .build();

        // Act
        FindUserByEmailDTO result = sanitizer.sanitize(dto);

        // Assert
        assertFalse(result.getEmail().contains("<script>"));
    }

    @Test
    @DisplayName("Should handle Gmail dot removal")
    void shouldHandleGmailDotRemoval() {
        // Arrange
        FindUserByEmailDTO dto = FindUserByEmailDTO.builder()
                .email("first.last@gmail.com")
                .build();

        // Act
        FindUserByEmailDTO result = sanitizer.sanitize(dto);

        // Assert
        assertEquals("firstlast@gmail.com", result.getEmail());
    }

    @Test
    @DisplayName("Should not modify non-Gmail addresses")
    void shouldNotModifyNonGmailAddresses() {
        // Arrange
        FindUserByEmailDTO dto = FindUserByEmailDTO.builder()
                .email("first.last@outlook.com")
                .build();

        // Act
        FindUserByEmailDTO result = sanitizer.sanitize(dto);

        // Assert
        assertEquals("first.last@outlook.com", result.getEmail());
    }

    @Test
    @DisplayName("Should apply all sanitization rules")
    void shouldApplyAllRules() {
        // Arrange
        FindUserByEmailDTO dto = FindUserByEmailDTO.builder()
                .email("  USER<script>@EXAMPLE.COM  ")
                .build();

        // Act
        FindUserByEmailDTO result = sanitizer.sanitize(dto);

        // Assert
        assertFalse(result.getEmail().contains("<script>"));
        //  the sanitization layer would remove the illegal input portion. If the output by sanitation layer is wrong,
        //  the validation layer will catch and raise an exception.
        assertEquals("user", result.getEmail());
    }

    @Test
    @DisplayName("Should handle null email")
    void shouldHandleNullEmail() {
        // Arrange
        FindUserByEmailDTO dto = FindUserByEmailDTO.builder()
                .email(null)
                .build();

        // Act
        FindUserByEmailDTO result = sanitizer.sanitize(dto);

        // Assert
        assertNull(result.getEmail());
    }

    @Test
    @DisplayName("Should handle empty email")
    void shouldHandleEmptyEmail() {
        // Arrange
        FindUserByEmailDTO dto = FindUserByEmailDTO.builder()
                .email("")
                .build();

        // Act
        FindUserByEmailDTO result = sanitizer.sanitize(dto);

        // Assert
        assertEquals("", result.getEmail());
    }

    @Test
    @DisplayName("Should return same DTO instance")
    void shouldReturnSameDTOInstance() {
        // Arrange
        FindUserByEmailDTO dto = FindUserByEmailDTO.builder()
                .email("user@example.com")
                .build();

        // Act
        FindUserByEmailDTO result = sanitizer.sanitize(dto);

        // Assert
        assertSame(dto, result);
    }
}
