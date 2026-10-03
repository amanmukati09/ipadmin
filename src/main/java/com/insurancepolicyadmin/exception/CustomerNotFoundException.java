package com.insurancepolicyadmin.exception;

/**
 * Thrown when a requested Customer identifier does not exist.
 * Validates: Requirements 3.5, 7.3.
 */
public class CustomerNotFoundException extends ApplicationException {

    public CustomerNotFoundException(String message) {
        super("CUSTOMER_NOT_FOUND", 404, message);
    }

    public CustomerNotFoundException(Long customerId) {
        super("CUSTOMER_NOT_FOUND", 404, "Customer not found: " + customerId);
    }
}
