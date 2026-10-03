package com.insurancepolicyadmin.exception;

/**
 * Minimal field-level error descriptor used by {@link ApplicationException} to carry
 * validation failures. A standalone placeholder in the exception package until
 * dto.response.ErrorResponse (Task 7.2) is finalized; mirrors the name/message shape
 * expected by ErrorResponse.fieldErrors[].
 */
public class FieldError {

    private String field;
    private String message;

    public FieldError() {
    }

    public FieldError(String field, String message) {
        this.field = field;
        this.message = message;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
