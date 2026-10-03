package com.insurancepolicyadmin.dto.response;

import com.insurancepolicyadmin.exception.FieldError;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Shared JSON shape for all error responses (design.md "REST API Design" section),
 * populated by {@code exception.GlobalExceptionHandler} (Task 7.4) from an
 * {@code ApplicationException}'s {@code errorCode}/{@code message}/{@code fieldErrors}.
 *
 * <p>Reuses {@link com.insurancepolicyadmin.exception.FieldError} for {@code fieldErrors}
 * rather than defining a duplicate type, since {@code ApplicationException} already
 * carries a {@code List<FieldError>} of that exact shape ({@code field}/{@code message}).</p>
 */
public class ErrorResponse {
    private String errorCode;
    private String message;
    private List<FieldError> fieldErrors;
    private LocalDateTime timestamp;

    public ErrorResponse() {
    }

    public ErrorResponse(String errorCode, String message, List<FieldError> fieldErrors, LocalDateTime timestamp) {
        this.errorCode = errorCode;
        this.message = message;
        this.fieldErrors = fieldErrors;
        this.timestamp = timestamp;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<FieldError> getFieldErrors() {
        return fieldErrors;
    }

    public void setFieldErrors(List<FieldError> fieldErrors) {
        this.fieldErrors = fieldErrors;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
