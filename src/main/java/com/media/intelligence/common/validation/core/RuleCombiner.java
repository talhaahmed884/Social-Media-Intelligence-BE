package com.media.intelligence.common.validation.core;

import com.media.intelligence.common.sanitization.core.SanitizationRule;
import com.media.intelligence.common.validation.rule.ValidationRule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Combines multiple validation rules (and optionally sanitization rules) for use with primitive types or single parameters.
 * Unlike Validator&lt;T&gt; which is for DTOs, RuleCombiner validates a single value directly.
 * <p>
 * Example usage (validation only):
 * <pre>
 * RuleCombiner&lt;String&gt; emailRules = new RuleCombiner&lt;&gt;(
 *     new RequiredRule(),
 *     new EmailFormatRule(),
 *     new StringLengthRule(5, 255)
 * );
 *
 * ValidationResult result = emailRules.validate(email, "email");
 * if (result.hasErrors()) {
 *     // Handle validation errors
 * }
 * </pre>
 * <p>
 * Example usage (sanitization + validation):
 * <pre>
 * RuleCombiner&lt;String&gt; emailRules = RuleCombiner.&lt;String&gt;builder()
 *     .sanitizationRules(
 *         new WhitespaceSanitizationRule(),
 *         new EmailNormalizationRule(),
 *         new XssSanitizationRule()
 *     )
 *     .validationRules(
 *         new RequiredRule(),
 *         new EmailFormatRule()
 *     )
 *     .build();
 *
 * ValidationResult result = emailRules.sanitizeAndValidate(email, "email");
 * String sanitizedValue = (String) result.getSanitizedTarget();
 * </pre>
 */
public class RuleCombiner<T> {

    private final List<ValidationRule<T>> validationRules;
    private final List<SanitizationRule<T>> sanitizationRules;

    /**
     * Create a RuleCombiner with only validation rules.
     *
     * @param rules the validation rules to combine
     */
    @SafeVarargs
    public RuleCombiner(ValidationRule<T>... rules) {
        this.validationRules = Arrays.asList(rules);
        this.sanitizationRules = Collections.emptyList();
    }

    /**
     * Private constructor for builder.
     */
    private RuleCombiner(List<ValidationRule<T>> validationRules, List<SanitizationRule<T>> sanitizationRules) {
        this.validationRules = validationRules;
        this.sanitizationRules = sanitizationRules;
    }

    /**
     * Create a builder for RuleCombiner with both sanitization and validation.
     *
     * @param <T> the type to validate/sanitize
     * @return a new builder instance
     */
    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    /**
     * Validate a single value against all combined validation rules.
     *
     * @param value     the value to validate
     * @param fieldName the name of the field (for error messages)
     * @return ValidationResult containing all errors or success
     */
    public ValidationResult validate(T value, String fieldName) {
        List<ValidationError> allErrors = new ArrayList<>();

        // Apply all validation rules to the value
        for (ValidationRule<T> rule : validationRules) {
            ValidationResult result = rule.validate(value, fieldName);
            if (result.hasErrors()) {
                allErrors.addAll(result.getErrors());
            }
        }

        // Return merged result
        if (allErrors.isEmpty()) {
            return ValidationResult.success(value);
        } else {
            return ValidationResult.failure(allErrors);
        }
    }

    /**
     * Sanitize then validate a value.
     * First applies all sanitization rules in sequence, then validates the sanitized value.
     *
     * @param value     the value to sanitize and validate
     * @param fieldName the name of the field (for error messages)
     * @return ValidationResult containing the sanitized value or errors
     */
    public ValidationResult sanitizeAndValidate(T value, String fieldName) {
        // Step 1: Sanitize the value
        T sanitized = value;
        for (SanitizationRule<T> rule : sanitizationRules) {
            sanitized = rule.sanitize(sanitized);
        }

        // Step 2: Validate the sanitized value
        List<ValidationError> allErrors = new ArrayList<>();
        for (ValidationRule<T> rule : validationRules) {
            ValidationResult result = rule.validate(sanitized, fieldName);
            if (result.hasErrors()) {
                allErrors.addAll(result.getErrors());
            }
        }

        // Return result with sanitized value
        if (allErrors.isEmpty()) {
            return ValidationResult.success(sanitized);
        } else {
            return ValidationResult.failure(allErrors);
        }
    }

    /**
     * Check if the value is valid (convenience method).
     *
     * @param value     the value to validate
     * @param fieldName the name of the field
     * @return true if all rules pass, false otherwise
     */
    public boolean isValid(T value, String fieldName) {
        return !validate(value, fieldName).hasErrors();
    }

    /**
     * Builder for RuleCombiner with both sanitization and validation rules.
     *
     * @param <T> the type to validate/sanitize
     */
    public static class Builder<T> {
        private List<SanitizationRule<T>> sanitizationRules = Collections.emptyList();
        private List<ValidationRule<T>> validationRules = Collections.emptyList();

        /**
         * Set the sanitization rules.
         *
         * @param rules the sanitization rules
         * @return this builder
         */
        @SafeVarargs
        public final Builder<T> sanitizationRules(SanitizationRule<T>... rules) {
            this.sanitizationRules = Arrays.asList(rules);
            return this;
        }

        /**
         * Set the validation rules.
         *
         * @param rules the validation rules
         * @return this builder
         */
        @SafeVarargs
        public final Builder<T> validationRules(ValidationRule<T>... rules) {
            this.validationRules = Arrays.asList(rules);
            return this;
        }

        /**
         * Build the RuleCombiner.
         *
         * @return a new RuleCombiner instance
         */
        public RuleCombiner<T> build() {
            return new RuleCombiner<>(validationRules, sanitizationRules);
        }
    }
}
