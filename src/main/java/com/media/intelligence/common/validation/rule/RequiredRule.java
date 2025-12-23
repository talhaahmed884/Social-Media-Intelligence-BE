package com.media.intelligence.common.validation.rule;

/**
 * Validates that a value is not null or blank.
 * Generic type allows it to work with any type while maintaining type safety.
 *
 * @param <T> the type being validated
 */
public class RequiredRule<T> extends ValidationRule<T> {

    public RequiredRule() {
        super("REQUIRED", "This field is required");
    }

    public RequiredRule(String customMessage) {
        super("REQUIRED", customMessage);
    }

    @Override
    protected boolean isValid(T value) {
        if (value == null) {
            return false;
        }
        if (value instanceof String) {
            return !((String) value).isBlank();
        }
        return true;
    }
}
