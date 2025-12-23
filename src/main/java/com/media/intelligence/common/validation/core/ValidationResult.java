package com.media.intelligence.common.validation.core;

import lombok.Builder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Container for validation results.
 * Immutable, thread-safe.
 */
@Getter
@Builder
public class ValidationResult {

    private final boolean valid;
    private final List<ValidationError> errors;
    private final Object sanitizedTarget; // The sanitized DTO

    /**
     * Create a success result.
     *
     * @param sanitizedTarget the sanitized object
     * @return success result
     */
    public static ValidationResult success(Object sanitizedTarget) {
        return ValidationResult.builder()
                .valid(true)
                .errors(Collections.emptyList())
                .sanitizedTarget(sanitizedTarget)
                .build();
    }

    /**
     * Create a failure result with a single error.
     *
     * @param error the validation error
     * @return failure result
     */
    public static ValidationResult failure(ValidationError error) {
        return ValidationResult.builder()
                .valid(false)
                .errors(Collections.singletonList(error))
                .build();
    }

    /**
     * Create a failure result with multiple errors.
     *
     * @param errors list of validation errors
     * @return failure result
     */
    public static ValidationResult failure(List<ValidationError> errors) {
        return ValidationResult.builder()
                .valid(false)
                .errors(errors)
                .build();
    }

    /**
     * Merge multiple validation results into one.
     *
     * @param results array of validation results
     * @return merged result
     */
    public static ValidationResult merge(ValidationResult... results) {
        List<ValidationError> allErrors = new ArrayList<>();
        Object sanitizedTarget = null;

        for (ValidationResult result : results) {
            if (result.hasErrors()) {
                allErrors.addAll(result.getErrors());
            }
            if (sanitizedTarget == null && result.getSanitizedTarget() != null) {
                sanitizedTarget = result.getSanitizedTarget();
            }
        }

        if (allErrors.isEmpty()) {
            return success(sanitizedTarget);
        } else {
            return failure(allErrors);
        }
    }

    /**
     * Check if validation has errors.
     *
     * @return true if there are validation errors
     */
    public boolean hasErrors() {
        return !valid || (errors != null && !errors.isEmpty());
    }

    /**
     * Get a comma-separated string of all error messages.
     *
     * @return formatted error messages (e.g., "Email is required, Password is too weak")
     */
    public String getErrorMessage() {
        return getErrorMessage("Validation failed");
    }

    /**
     * Get a comma-separated string of all error messages with a fallback.
     *
     * @param fallback the message to return if no errors exist
     * @return formatted error messages or fallback
     */
    public String getErrorMessage(String fallback) {
        if (errors == null || errors.isEmpty()) {
            return fallback;
        }
        return errors.stream()
                .map(ValidationError::getMessage)
                .reduce((a, b) -> a + ", " + b)
                .orElse(fallback);
    }

    /**
     * Get a detailed error message with field names.
     *
     * @return formatted error messages (e.g., "email: Email is required, password: Password is too weak")
     */
    public String getDetailedErrorMessage() {
        return getDetailedErrorMessage("Validation failed");
    }

    /**
     * Get a detailed error message with field names and a fallback.
     *
     * @param fallback the message to return if no errors exist
     * @return formatted error messages with field names or fallback
     */
    public String getDetailedErrorMessage(String fallback) {
        if (errors == null || errors.isEmpty()) {
            return fallback;
        }
        return errors.stream()
                .map(error -> error.getField() + ": " + error.getMessage())
                .reduce((a, b) -> a + ", " + b)
                .orElse(fallback);
    }
}
