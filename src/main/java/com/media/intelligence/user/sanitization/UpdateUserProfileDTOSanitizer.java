package com.media.intelligence.user.sanitization;

import com.media.intelligence.common.sanitization.core.Sanitizer;
import com.media.intelligence.common.sanitization.rule.EmailNormalizationRule;
import com.media.intelligence.common.sanitization.rule.WhitespaceSanitizationRule;
import com.media.intelligence.common.sanitization.rule.XssSanitizationRule;
import com.media.intelligence.user.dto.UpdateUserProfileDTO;
import org.springframework.stereotype.Component;

/**
 * Sanitizer for UpdateUserProfileDTO that combines sanitization rules.
 * <p>
 * Sanitization rules:
 * - email: Trim whitespace, normalize (lowercase, Gmail dot removal), XSS protection
 * - fullName: Trim whitespace, XSS protection
 */
@Component
public class UpdateUserProfileDTOSanitizer {
    private final Sanitizer<UpdateUserProfileDTO> sanitizer;

    public UpdateUserProfileDTOSanitizer() {
        this.sanitizer = new Sanitizer<UpdateUserProfileDTO>()
                .field("email",
                        new WhitespaceSanitizationRule(),
                        new EmailNormalizationRule(),
                        new XssSanitizationRule())
                .field("fullName",
                        new WhitespaceSanitizationRule(),
                        new XssSanitizationRule());
    }

    /**
     * Sanitize the UpdateUserProfileDTO.
     * The DTO is modified in place.
     *
     * @param dto the DTO to sanitize
     * @return the sanitized DTO (same instance)
     */
    public UpdateUserProfileDTO sanitize(UpdateUserProfileDTO dto) {
        return sanitizer.sanitize(dto);
    }
}
