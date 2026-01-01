package com.media.intelligence.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for user registration.
 * <p>
 * Validation and sanitization are handled by RegisterUserDTOValidator and RegisterUserDTOSanitizer.
 * <p>
 * Sanitization rules:
 * - Email: Whitespace trim, lowercase normalization, Gmail dot removal, XSS protection
 * - Password: Whitespace trim
 * - Full name: Whitespace trim, XSS protection
 * <p>
 * Validation rules:
 * - Email: Required, valid email format (RFC 5322), max 255 chars
 * - Password: Required, STRONG complexity (8+ chars, uppercase, lowercase, digit, special)
 * - Full name: Required, 2-255 chars
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterUserDTO {
    private String email;
    private String password;
    private String fullName;
}
