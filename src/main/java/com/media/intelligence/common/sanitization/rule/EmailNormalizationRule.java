package com.media.intelligence.common.sanitization.rule;

import com.media.intelligence.common.sanitization.core.SanitizationRule;

/**
 * Normalizes email addresses (lowercase, remove Gmail dots).
 */
public class EmailNormalizationRule extends SanitizationRule<String> {
    public EmailNormalizationRule() {
        super("EMAIL_NORMALIZATION");
    }

    @Override
    protected String sanitizeValue(String value) {
        // Convert to lowercase
        String normalized = value.toLowerCase();

        // Remove dots from Gmail local part
        if (normalized.contains("@")) {
            String[] parts = normalized.split("@");
            if (parts.length == 2) {
                String localPart = parts[0];
                String domain = parts[1];

                // Gmail/Googlemail: dots in local part are ignored
                if (domain.equals("gmail.com") || domain.equals("googlemail.com")) {
                    localPart = localPart.replace(".", "");
                }

                normalized = localPart + "@" + domain;
            }
        }

        return normalized;
    }
}
