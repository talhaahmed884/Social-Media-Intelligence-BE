package com.media.intelligence.common.validation.rule;

import com.media.intelligence.common.validation.core.ValidationResult;
import lombok.Getter;

/**
 * Base template for validation rules.
 * <p>
 * A validation rule is a single, composable validation check (e.g., "is required", "is email").
 * Rules can be combined to create complex validators.
 * <p>
 * Template Method Pattern: Subclasses implement validateValue() logic.
 *
 * @param <T> the type being validated
 */
@Getter
public abstract class ValidationRule<T> {
    private final String errorCode;
    private final String errorMessage;

    protected ValidationRule(String errorCode, String errorMessage) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    /**
     * Validate a value against this rule.
     *
     * @param value     the value to validate
     * @param fieldName the field name (for error messages)
     * @return validation result
     */
    public ValidationResult validate(T value, String fieldName) {
        if (isValid(value)) {
            return ValidationResult.success(value);
        }
        return ValidationResult.failure(
                com.media.intelligence.common.validation.core.ValidationError.builder()
                        .field(fieldName)
                        .rejectedValue(value != null ? value.toString() : null)
                        .message(errorMessage)
                        .code(errorCode)
                        .type(com.media.intelligence.common.validation.core.ValidationError.ValidationType.FIELD)
                        .build()
        );
    }

    /**
     * Implement validation logic.
     *
     * @param value the value to validate
     * @return true if valid, false otherwise
     */
    protected abstract boolean isValid(T value);
}
