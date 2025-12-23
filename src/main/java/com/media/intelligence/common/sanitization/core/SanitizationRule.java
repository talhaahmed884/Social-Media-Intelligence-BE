package com.media.intelligence.common.sanitization.core;

import lombok.Getter;

/**
 * Base template for sanitization rules.
 * <p>
 * A sanitization rule transforms/cleans input (e.g., "trim whitespace", "remove HTML").
 * Rules can be chained to create complex sanitization pipelines.
 * <p>
 * Template Method Pattern: Subclasses implement sanitizeValue() logic.
 *
 * @param <T> the type being sanitized
 */
@Getter
public abstract class SanitizationRule<T> {
    private final String ruleName;

    protected SanitizationRule(String ruleName) {
        this.ruleName = ruleName;
    }

    /**
     * Sanitize a value.
     *
     * @param value the value to sanitize
     * @return sanitized value
     */
    public T sanitize(T value) {
        if (value == null) {
            return null;
        }
        return sanitizeValue(value);
    }

    /**
     * Implement sanitization logic.
     *
     * @param value the non-null value to sanitize
     * @return sanitized value
     */
    protected abstract T sanitizeValue(T value);
}
