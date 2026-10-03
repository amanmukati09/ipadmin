package com.insurancepolicyadmin.exception;

import java.util.Collections;
import java.util.List;

/**
 * Base class for all application-specific exceptions. Unchecked (extends RuntimeException)
 * so service/DAO methods do not need to declare it. Carries an errorCode and httpStatus so
 * GlobalExceptionHandler (Task 7.4) can map instances to a consistent JSON error response,
 * plus an optional list of field-level validation errors.
 */
public class ApplicationException extends RuntimeException {

    private final String errorCode;
    private final int httpStatus;
    private final List<FieldError> fieldErrors;

    public ApplicationException(String errorCode, int httpStatus, String message) {
        this(errorCode, httpStatus, message, Collections.emptyList());
    }

    public ApplicationException(String errorCode, int httpStatus, String message, List<FieldError> fieldErrors) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.fieldErrors = fieldErrors != null ? fieldErrors : Collections.emptyList();
    }

    public String getErrorCode() {
        return errorCode;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public List<FieldError> getFieldErrors() {
        return fieldErrors;
    }
}
