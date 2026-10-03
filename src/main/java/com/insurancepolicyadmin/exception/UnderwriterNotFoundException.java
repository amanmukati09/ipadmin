package com.insurancepolicyadmin.exception;

/**
 * Thrown when a requested Underwriter identifier does not exist.
 * Validates: Requirements 5.4.
 */
public class UnderwriterNotFoundException extends ApplicationException {

    public UnderwriterNotFoundException(String message) {
        super("UNDERWRITER_NOT_FOUND", 404, message);
    }

    public UnderwriterNotFoundException(Long underwriterId) {
        super("UNDERWRITER_NOT_FOUND", 404, "Underwriter not found: " + underwriterId);
    }
}
