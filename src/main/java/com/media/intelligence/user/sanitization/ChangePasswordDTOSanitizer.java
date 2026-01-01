package com.media.intelligence.user.sanitization;

import com.media.intelligence.common.sanitization.core.Sanitizer;
import com.media.intelligence.common.sanitization.rule.WhitespaceSanitizationRule;
import com.media.intelligence.user.dto.ChangePasswordDTO;
import org.springframework.stereotype.Component;

/**
 * Sanitizer for ChangePasswordDTO that combines sanitization rules.
 * <p>
 * Sanitization rules:
 * - currentPassword: Trim whitespace only (preserve special characters)
 * - newPassword: Trim whitespace only (preserve special characters)
 */
@Component
public class ChangePasswordDTOSanitizer {
    private final Sanitizer<ChangePasswordDTO> sanitizer;

    public ChangePasswordDTOSanitizer() {
        this.sanitizer = new Sanitizer<ChangePasswordDTO>()
                .field("currentPassword",
                        new WhitespaceSanitizationRule())
                .field("newPassword",
                        new WhitespaceSanitizationRule());
    }

    /**
     * Sanitize the ChangePasswordDTO.
     * The DTO is modified in place.
     *
     * @param dto the DTO to sanitize
     * @return the sanitized DTO (same instance)
     */
    public ChangePasswordDTO sanitize(ChangePasswordDTO dto) {
        return sanitizer.sanitize(dto);
    }
}
