package com.media.intelligence.common.exception;

import com.media.intelligence.common.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Global exception handler that catches all exceptions and returns standardized ApiResponse.
 * Uses @RestControllerAdvice to handle exceptions across all controllers.
 * <p>
 * The handler dynamically extracts ErrorCode from BaseException without knowing
 * the specific exception type, enabling domain-specific error handling.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handle all BaseException subclasses.
     * Dynamically extracts ErrorCode regardless of the specific exception type.
     * This allows each domain to have its own exception types (UserNotFoundException, etc.)
     * without modifying this handler.
     */
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ApiResponse<Void>> handleBaseException(
            BaseException ex,
            WebRequest request) {

        ErrorCode errorCode = ex.getErrorCode();

        log.warn("Business exception: {} - {}", errorCode.getCode(), errorCode.getMessage(), ex);

        ApiResponse<Void> response = ApiResponse.error(
                errorCode.getCode(),
                errorCode.getMessage()
        );

        return new ResponseEntity<>(response, ex.getHttpStatus());
    }

    /**
     * Handle validation errors from @Valid annotation (Jakarta validation).
     * Maps to CommonErrorCode.VALIDATION_FAILED.
     * <p>
     * Note: This will be deprecated once migration to custom framework is complete.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(
            MethodArgumentNotValidException ex,
            WebRequest request) {

        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.toList());

        log.warn("Validation failed: {}", errors);

        ApiResponse<Void> response = ApiResponse.error(
                CommonErrorCode.VALIDATION_FAILED.getCode(),
                CommonErrorCode.VALIDATION_FAILED.getMessage(),
                errors
        );

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle illegal argument exceptions.
     * Maps to CommonErrorCode.INVALID_INPUT.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(
            IllegalArgumentException ex,
            WebRequest request) {

        log.warn("Illegal argument: {}", ex.getMessage());

        ApiResponse<Void> response = ApiResponse.error(
                CommonErrorCode.INVALID_INPUT.getCode(),
                ex.getMessage()
        );

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle all other uncaught exceptions.
     * Maps to CommonErrorCode.INTERNAL_SERVER_ERROR.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGlobalException(
            Exception ex,
            WebRequest request) {

        log.error("Unexpected error occurred", ex);

        ApiResponse<Void> response = ApiResponse.error(
                CommonErrorCode.INTERNAL_SERVER_ERROR.getCode(),
                CommonErrorCode.INTERNAL_SERVER_ERROR.getMessage()
        );

        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Handle malformed JSON in request body
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            WebRequest request) {

        log.error("Malformed JSON request", ex);

        ApiResponse<Void> errorResponse = ApiResponse.error(
                CommonErrorCode.MALFORMED_JSON.getCode(),
                CommonErrorCode.MALFORMED_JSON.getMessage()
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle wrong HTTP method (e.g., GET instead of POST)
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex,
            WebRequest request) {

        log.error("HTTP method not supported", ex);

        ApiResponse<Void> errorResponse = ApiResponse.error(
                CommonErrorCode.METHOD_NOT_ALLOWED.getCode(),
                CommonErrorCode.METHOD_NOT_ALLOWED.getMessage()
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.METHOD_NOT_ALLOWED);
    }

    /**
     * Handle missing request parameters
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParams(
            MissingServletRequestParameterException ex,
            WebRequest request) {

        log.error("Missing request parameter", ex);

        ApiResponse<Void> errorResponse = ApiResponse.error(
                CommonErrorCode.MISSING_PARAMETER.getCode(),
                CommonErrorCode.MISSING_PARAMETER.getMessage()
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
}
