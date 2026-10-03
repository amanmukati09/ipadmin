package com.insurancepolicyadmin.exception;

/**
 * Thrown when a new Customer's email or phone exactly matches an existing Customer record.
 * Validates: Requirements 3.8.
 */
public class DuplicateCustomerException extends ApplicationException {

    public DuplicateCustomerException(String message) {
        super("DUPLICATE_CUSTOMER", 409, message);
    }
}
