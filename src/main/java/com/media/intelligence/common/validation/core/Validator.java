package com.media.intelligence.common.validation.core;

import com.media.intelligence.common.validation.rule.ValidationRule;

import java.lang.reflect.Field;
import java.util.*;

/**
 * Base template class for validating DTOs by combining validation rules.
 * Uses composition to apply multiple rules to different fields.
 * <p>
 * Example usage:
 * <pre>
 * Validator&lt;RegisterUserDTO&gt; validator = new Validator&lt;&gt;()
 *     .field("email", new RequiredRule(), new EmailFormatRule())
 *     .field("password", new RequiredRule(), PasswordStrengthRule.strong());
 *
 * ValidationResult result = validator.validate(dto);
 * </pre>
 */
public class Validator<T> {
    private final Map<String, List<ValidationRule<?>>> fieldRules;

    public Validator() {
        this.fieldRules = new LinkedHashMap<>();
    }

    /**
     * Add validation rules for a specific field.
     *
     * @param fieldName the name of the field to validate
     * @param rules     the validation rules to apply
     * @return this validator for method chaining
     */
    public Validator<T> field(String fieldName, ValidationRule<?>... rules) {
        fieldRules.computeIfAbsent(fieldName, k -> new ArrayList<>()).addAll(Arrays.asList(rules));
        return this;
    }

    /**
     * Validate the target DTO against all configured field rules.
     *
     * @param target the DTO to validate
     * @return ValidationResult containing all errors or success
     */
    public ValidationResult validate(T target) {
        if (target == null) {
            return ValidationResult.failure(
                    ValidationError.builder()
                            .field(null)
                            .rejectedValue("null")
                            .message("Target object is null")
                            .code("NULL_TARGET")
                            .type(ValidationError.ValidationType.FIELD)
                            .build()
            );
        }

        List<ValidationError> allErrors = new ArrayList<>();

        // Validate each field with its configured rules
        for (Map.Entry<String, List<ValidationRule<?>>> entry : fieldRules.entrySet()) {
            String fieldName = entry.getKey();
            List<ValidationRule<?>> rules = entry.getValue();

            // Get field value using reflection
            Object fieldValue = getFieldValue(target, fieldName);

            // Apply all rules for this field
            for (ValidationRule<?> rule : rules) {
                ValidationResult result = validateField(rule, fieldValue, fieldName);
                if (result.hasErrors()) {
                    allErrors.addAll(result.getErrors());
                }
            }
        }

        // Return merged result
        if (allErrors.isEmpty()) {
            return ValidationResult.success(target);
        } else {
            return ValidationResult.failure(allErrors);
        }
    }

    /**
     * Validate a single field with a single rule.
     */
    @SuppressWarnings("unchecked")
    private <V> ValidationResult validateField(ValidationRule<V> rule, Object fieldValue, String fieldName) {
        try {
            return rule.validate((V) fieldValue, fieldName);
        } catch (ClassCastException e) {
            return ValidationResult.failure(
                    ValidationError.builder()
                            .field(fieldName)
                            .rejectedValue(fieldValue != null ? fieldValue.toString() : null)
                            .message("Type mismatch for validation rule")
                            .code("TYPE_MISMATCH")
                            .type(ValidationError.ValidationType.FIELD)
                            .build()
            );
        }
    }

    /**
     * Get field value from target object using reflection.
     */
    private Object getFieldValue(T target, String fieldName) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(target);
        } catch (NoSuchFieldException e) {
            throw new IllegalArgumentException("Field '" + fieldName + "' does not exist in " + target.getClass().getSimpleName());
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Cannot access field '" + fieldName + "'", e);
        }
    }
}
