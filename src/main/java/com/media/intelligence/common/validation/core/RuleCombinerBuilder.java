package com.media.intelligence.common.validation.core;

import com.media.intelligence.common.sanitization.core.SanitizationRule;
import com.media.intelligence.common.validation.rule.ValidationRule;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Builder for RuleCombiner with both sanitization and validation rules.
 *
 * @param <T> the type to validate/sanitize
 */
public class RuleCombinerBuilder<T> {
    protected List<SanitizationRule<T>> sanitizationRules = Collections.emptyList();
    protected List<ValidationRule<T>> validationRules = Collections.emptyList();

    /**
     * Set the sanitization rules.
     *
     * @param rules the sanitization rules
     * @return this builder
     */
    @SafeVarargs
    public final RuleCombinerBuilder<T> sanitizationRules(SanitizationRule<T>... rules) {
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
    public final RuleCombinerBuilder<T> validationRules(ValidationRule<T>... rules) {
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
