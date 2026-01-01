package com.media.intelligence.common.validation.rule;

import java.util.regex.Pattern;

/**
 * Validates email format (RFC 5322 compliant).
 */
public class EmailFormatRule extends ValidationRule<String> {
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@" +
                    "(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}$"
    );

    public EmailFormatRule() {
        super("INVALID_EMAIL", "Email must be valid");
    }

    public EmailFormatRule(String customMessage) {
        super("INVALID_EMAIL", customMessage);
    }

    @Override
    protected boolean isValid(String value) {
        if (value == null || value.isBlank()) {
            return false; // Use RequiredRule for null checks
        }
        return EMAIL_PATTERN.matcher(value).matches();
    }
}
