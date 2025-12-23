package com.media.intelligence.common.validation.rule;

/**
 * Validates string length (min and max).
 */
public class StringLengthRule extends ValidationRule<String> {

    private final int min;
    private final int max;

    public StringLengthRule(int min, int max) {
        super("INVALID_LENGTH", String.format("Must be between %d and %d characters", min, max));
        this.min = min;
        this.max = max;
    }

    public StringLengthRule(int min, int max, String customMessage) {
        super("INVALID_LENGTH", customMessage);
        this.min = min;
        this.max = max;
    }

    @Override
    protected boolean isValid(String value) {
        if (value == null) {
            return true; // Use RequiredRule for null checks
        }
        int length = value.length();
        return length >= min && length <= max;
    }
}
