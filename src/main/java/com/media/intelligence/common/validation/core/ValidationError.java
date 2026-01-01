package com.media.intelligence.common.validation.core;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Represents a single validation error.
 * Compatible with GlobalExceptionHandler format.
 */
@Getter
@Builder
@AllArgsConstructor
public class ValidationError {
    private final String field;           // "email", "password", etc.
    private final String rejectedValue;   // The value that failed
    private final String message;         // "Email is required"
    private final String code;            // "REQUIRED", "INVALID_EMAIL", etc.
    private final ValidationType type;    // FIELD, OBJECT, CROSS_FIELD

    /**
     * Format for GlobalExceptionHandler compatibility.
     * Returns: "field: message"
     *
     * @return formatted error string
     */
    public String toFormattedString() {
        return field + ": " + message;
    }

    /**
     * Validation type enumeration.
     */
    public enum ValidationType {
        FIELD,
        OBJECT,
        CROSS_FIELD
    }
}
