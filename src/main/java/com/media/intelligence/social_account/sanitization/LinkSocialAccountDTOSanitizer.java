package com.media.intelligence.social_account.sanitization;

import com.media.intelligence.common.sanitization.core.Sanitizer;
import com.media.intelligence.common.sanitization.rule.WhitespaceSanitizationRule;
import com.media.intelligence.social_account.dto.LinkSocialAccountDTO;
import org.springframework.stereotype.Component;

/**
 * Sanitizer for LinkSocialAccountDTO.
 * <p>
 * Sanitization rules:
 * - accessToken: Trim whitespace only (tokens are opaque; no XSS rule)
 */
@Component
public class LinkSocialAccountDTOSanitizer {
    private final Sanitizer<LinkSocialAccountDTO> sanitizer;

    public LinkSocialAccountDTOSanitizer() {
        this.sanitizer = new Sanitizer<LinkSocialAccountDTO>()
                .field("accessToken", new WhitespaceSanitizationRule());
    }

    public LinkSocialAccountDTO sanitize(LinkSocialAccountDTO dto) {
        return sanitizer.sanitize(dto);
    }
}
