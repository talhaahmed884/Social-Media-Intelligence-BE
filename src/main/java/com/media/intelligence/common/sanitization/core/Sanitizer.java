package com.media.intelligence.common.sanitization.core;

import java.lang.reflect.Field;
import java.util.*;

/**
 * Base template class for sanitizing DTOs by combining sanitization rules.
 * Uses composition to apply multiple rules to different fields.
 * <p>
 * Example usage:
 * <pre>
 * Sanitizer&lt;RegisterUserDTO&gt; sanitizer = new Sanitizer&lt;&gt;()
 *     .field("email", new WhitespaceSanitizationRule(), new EmailNormalizationRule())
 *     .field("fullName", new WhitespaceSanitizationRule(), new XssSanitizationRule());
 *
 * RegisterUserDTO sanitized = sanitizer.sanitize(dto);
 * </pre>
 */
public class Sanitizer<T> {

    private final Map<String, List<SanitizationRule<?>>> fieldRules;

    public Sanitizer() {
        this.fieldRules = new LinkedHashMap<>();
    }

    /**
     * Add sanitization rules for a specific field.
     *
     * @param fieldName the name of the field to sanitize
     * @param rules     the sanitization rules to apply
     * @return this sanitizer for method chaining
     */
    public Sanitizer<T> field(String fieldName, SanitizationRule<?>... rules) {
        fieldRules.computeIfAbsent(fieldName, k -> new ArrayList<>()).addAll(Arrays.asList(rules));
        return this;
    }

    /**
     * Sanitize the target DTO by applying all configured field rules.
     * The original DTO is modified in place.
     *
     * @param target the DTO to sanitize
     * @return the sanitized DTO (same instance, modified in place)
     */
    public T sanitize(T target) {
        if (target == null) {
            return null;
        }

        // Sanitize each field with its configured rules
        for (Map.Entry<String, List<SanitizationRule<?>>> entry : fieldRules.entrySet()) {
            String fieldName = entry.getKey();
            List<SanitizationRule<?>> rules = entry.getValue();

            // Apply all rules for this field in sequence
            Object sanitizedValue = getFieldValue(target, fieldName);
            for (SanitizationRule<?> rule : rules) {
                sanitizedValue = sanitizeField(rule, sanitizedValue);
            }

            // Set the sanitized value back to the field
            setFieldValue(target, fieldName, sanitizedValue);
        }

        return target;
    }

    /**
     * Sanitize a single field value with a single rule.
     */
    @SuppressWarnings("unchecked")
    private <V> V sanitizeField(SanitizationRule<V> rule, Object fieldValue) {
        try {
            return rule.sanitize((V) fieldValue);
        } catch (ClassCastException e) {
            // If type mismatch, return original value
            return (V) fieldValue;
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

    /**
     * Set field value on target object using reflection.
     */
    private void setFieldValue(T target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (NoSuchFieldException e) {
            throw new IllegalArgumentException("Field '" + fieldName + "' does not exist in " + target.getClass().getSimpleName());
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Cannot access field '" + fieldName + "'", e);
        }
    }
}
