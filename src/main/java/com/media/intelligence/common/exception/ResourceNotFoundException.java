package com.media.intelligence.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a requested resource is not found.
 * Uses CommonErrorCode.RESOURCE_NOT_FOUND.
 * For domain-specific "not found" exceptions, consider creating
 * domain-specific exceptions with domain-specific error codes.
 */
public class ResourceNotFoundException extends BaseException {
    public ResourceNotFoundException() {
        super(CommonErrorCode.RESOURCE_NOT_FOUND);
    }

    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(
                new ErrorCode() {
                    @Override
                    public String getCode() {
                        return CommonErrorCode.RESOURCE_NOT_FOUND.getCode();
                    }

                    @Override
                    public String getMessage() {
                        return String.format("%s not found with %s: '%s'", resourceName, fieldName, fieldValue);
                    }

                    @Override
                    public HttpStatus getHttpStatus() {
                        return HttpStatus.NOT_FOUND;
                    }
                }
        );
    }

    public ResourceNotFoundException(Throwable cause) {
        super(CommonErrorCode.RESOURCE_NOT_FOUND, cause);
    }
}
