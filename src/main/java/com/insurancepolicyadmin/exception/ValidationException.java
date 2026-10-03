package com.insurancepolicyadmin.exception;

import java.util.List;

/**
 * Thrown for field-level validation failures across Customer, Agent, Beneficiary, Policy,
 * Endorsement, Payment, and Report request handling.
 * Validates: Requirements 3.3, 3.8, 4.2, 6.3, 6.4, 7.4, 9.3, 9.4, 11.4, 11.5, 12.2, 13.2.
 */
public class ValidationException extends ApplicationException {

    public ValidationException(String message) {
        super("VALIDATION_ERROR", 400, message);
    }

    public ValidationException(String message, List<FieldError> fieldErrors) {
        super("VALIDATION_ERROR", 400, message, fieldErrors);
    }
}
