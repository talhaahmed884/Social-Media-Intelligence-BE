package com.media.intelligence.social_account.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Request to link a social account with an existing access token
 * (e.g. a long-lived token generated in the Meta App Dashboard), bypassing the OAuth redirect.
 * <p>
 * Mutable class (not a record) because the common Sanitizer writes fields in place.
 * <p>
 * Sanitization: accessToken whitespace trim.
 * Validation: accessToken required, 1-1024 chars.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LinkSocialAccountDTO {
    @ToString.Exclude
    private String accessToken;
}
