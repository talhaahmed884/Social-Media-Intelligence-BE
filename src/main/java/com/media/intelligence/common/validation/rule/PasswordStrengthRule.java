package com.media.intelligence.common.validation.rule;

/**
 * Validates password strength with configurable requirements.
 */
public class PasswordStrengthRule extends ValidationRule<String> {
    private final int minLength;
    private final boolean requireUppercase;
    private final boolean requireLowercase;
    private final boolean requireDigit;
    private final boolean requireSpecialChar;
    private final String allowedSpecialChars;

    public PasswordStrengthRule(int minLength, boolean requireUppercase, boolean requireLowercase,
                                boolean requireDigit, boolean requireSpecialChar) {
        super("WEAK_PASSWORD", "Password does not meet security requirements");
        this.minLength = minLength;
        this.requireUppercase = requireUppercase;
        this.requireLowercase = requireLowercase;
        this.requireDigit = requireDigit;
        this.requireSpecialChar = requireSpecialChar;
        this.allowedSpecialChars = "!@#$%^&*()_+-=[]{}|;:',.<>?/";
    }

    /**
     * Create with STRONG preset (8+ chars, uppercase, lowercase, digit, special).
     */
    public static PasswordStrengthRule strong() {
        return new PasswordStrengthRule(8, true, true, true, true);
    }

    /**
     * Create with MEDIUM preset (8+ chars, uppercase, lowercase).
     */
    public static PasswordStrengthRule medium() {
        return new PasswordStrengthRule(8, true, true, false, false);
    }

    /**
     * Create with BASIC preset (6+ chars, no special requirements).
     */
    public static PasswordStrengthRule basic() {
        return new PasswordStrengthRule(6, false, false, false, false);
    }

    @Override
    protected boolean isValid(String value) {
        if (value == null || value.isBlank()) {
            return true; // Use RequiredRule for null checks
        }

        if (value.length() < minLength) {
            return false;
        }

        if (requireUppercase && !containsUppercase(value)) {
            return false;
        }

        if (requireLowercase && !containsLowercase(value)) {
            return false;
        }

        if (requireDigit && !containsDigit(value)) {
            return false;
        }

        return !requireSpecialChar || containsSpecialChar(value);
    }

    private boolean containsUppercase(String password) {
        return password.chars().anyMatch(Character::isUpperCase);
    }

    private boolean containsLowercase(String password) {
        return password.chars().anyMatch(Character::isLowerCase);
    }

    private boolean containsDigit(String password) {
        return password.chars().anyMatch(Character::isDigit);
    }

    private boolean containsSpecialChar(String password) {
        return password.chars().anyMatch(c -> allowedSpecialChars.indexOf(c) >= 0);
    }
}
