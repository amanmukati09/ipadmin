package com.insurancepolicyadmin.exception;

/**
 * Thrown when an Agent or Underwriter license number already exists for another record.
 * Validates: Requirements 4.2.
 */
public class DuplicateLicenseException extends ApplicationException {

    public DuplicateLicenseException(String message) {
        super("DUPLICATE_LICENSE", 409, message);
    }
}
