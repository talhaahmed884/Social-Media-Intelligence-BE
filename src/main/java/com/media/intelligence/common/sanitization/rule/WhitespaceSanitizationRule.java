package com.media.intelligence.common.sanitization.rule;

import com.media.intelligence.common.sanitization.core.SanitizationRule;

/**
 * Trims whitespace from start and end.
 */
public class WhitespaceSanitizationRule extends SanitizationRule<String> {

    public WhitespaceSanitizationRule() {
        super("WHITESPACE_SANITIZATION");
    }

    @Override
    protected String sanitizeValue(String value) {
        return value.trim();
    }
}
