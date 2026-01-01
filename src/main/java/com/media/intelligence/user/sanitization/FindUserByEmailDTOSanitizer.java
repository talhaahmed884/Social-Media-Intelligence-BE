package com.media.intelligence.user.sanitization;

import com.media.intelligence.common.sanitization.core.Sanitizer;
import com.media.intelligence.common.sanitization.rule.EmailNormalizationRule;
import com.media.intelligence.common.sanitization.rule.WhitespaceSanitizationRule;
import com.media.intelligence.common.sanitization.rule.XssSanitizationRule;
import com.media.intelligence.user.dto.FindUserByEmailDTO;
import org.springframework.stereotype.Component;

/**
 * Sanitizer for FindUserByEmailDTO that combines sanitization rules.
 * <p>
 * Sanitization rules:
 * - email: Trim whitespace, normalize (lowercase, Gmail dot removal), XSS protection
 */
@Component
public class FindUserByEmailDTOSanitizer {
    private final Sanitizer<FindUserByEmailDTO> sanitizer;

    public FindUserByEmailDTOSanitizer() {
        this.sanitizer = new Sanitizer<FindUserByEmailDTO>()
                .field("email",
                        new WhitespaceSanitizationRule(),
                        new EmailNormalizationRule(),
                        new XssSanitizationRule());
    }

    /**
     * Sanitize the FindUserByEmailDTO.
     * The DTO is modified in place.
     *
     * @param dto the DTO to sanitize
     * @return the sanitized DTO (same instance)
     */
    public FindUserByEmailDTO sanitize(FindUserByEmailDTO dto) {
        return sanitizer.sanitize(dto);
    }
}
