package com.media.intelligence.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for finding a user by email.
 * <p>
 * Validation and sanitization are handled by FindUserByEmailDTOValidator and FindUserByEmailDTOSanitizer.
 * <p>
 * Sanitization rules:
 * - Email: Whitespace trim, lowercase normalization, Gmail dot removal, XSS protection
 * <p>
 * Validation rules:
 * - Email: Required, valid email format (RFC 5322), max 255 chars
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FindUserByEmailDTO {

    private String email;
}
