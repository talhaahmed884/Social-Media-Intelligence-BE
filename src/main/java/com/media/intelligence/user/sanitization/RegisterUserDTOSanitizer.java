package com.media.intelligence.user.sanitization;

import com.media.intelligence.common.sanitization.core.Sanitizer;
import com.media.intelligence.common.sanitization.rule.EmailNormalizationRule;
import com.media.intelligence.common.sanitization.rule.WhitespaceSanitizationRule;
import com.media.intelligence.common.sanitization.rule.XssSanitizationRule;
import com.media.intelligence.user.dto.RegisterUserDTO;
import org.springframework.stereotype.Component;

/**
 * Sanitizer for RegisterUserDTO that combines sanitization rules.
 * <p>
 * Sanitization rules:
 * - email: Trim whitespace, normalize (lowercase, Gmail dot removal), XSS protection
 * - password: Trim whitespace only (preserve special characters)
 * - fullName: Trim whitespace, XSS protection
 */
@Component
public class RegisterUserDTOSanitizer {
    private final Sanitizer<RegisterUserDTO> sanitizer;

    public RegisterUserDTOSanitizer() {
        this.sanitizer = new Sanitizer<RegisterUserDTO>()
                .field("email",
                        new WhitespaceSanitizationRule(),
                        new EmailNormalizationRule(),
                        new XssSanitizationRule())
                .field("password",
                        new WhitespaceSanitizationRule())
                .field("fullName",
                        new WhitespaceSanitizationRule(),
                        new XssSanitizationRule());
    }

    /**
     * Sanitize the RegisterUserDTO.
     * The DTO is modified in place.
     *
     * @param dto the DTO to sanitize
     * @return the sanitized DTO (same instance)
     */
    public RegisterUserDTO sanitize(RegisterUserDTO dto) {
        return sanitizer.sanitize(dto);
    }
}
