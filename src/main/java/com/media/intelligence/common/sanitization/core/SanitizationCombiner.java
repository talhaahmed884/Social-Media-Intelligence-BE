package com.media.intelligence.common.sanitization.core;

import java.util.Arrays;
import java.util.List;

/**
 * Combines multiple sanitization rules for use with primitive types or single parameters.
 * Unlike Sanitizer&lt;T&gt; which is for DTOs, SanitizationCombiner sanitizes a single value directly.
 * <p>
 * Example usage:
 * <pre>
 * SanitizationCombiner&lt;String&gt; emailSanitizer = new SanitizationCombiner&lt;&gt;(
 *     new WhitespaceSanitizationRule(),
 *     new EmailNormalizationRule(),
 *     new XssSanitizationRule()
 * );
 *
 * String sanitized = emailSanitizer.sanitize(email);
 * </pre>
 */
public class SanitizationCombiner<T> {
    private final List<SanitizationRule<T>> rules;

    /**
     * Create a SanitizationCombiner with the specified sanitization rules.
     *
     * @param rules the sanitization rules to combine
     */
    @SafeVarargs
    public SanitizationCombiner(SanitizationRule<T>... rules) {
        this.rules = Arrays.asList(rules);
    }

    /**
     * Sanitize a single value by applying all combined rules in sequence.
     *
     * @param value the value to sanitize
     * @return the sanitized value
     */
    public T sanitize(T value) {
        T result = value;

        // Apply all rules in sequence
        for (SanitizationRule<T> rule : rules) {
            result = rule.sanitize(result);
        }

        return result;
    }
}
