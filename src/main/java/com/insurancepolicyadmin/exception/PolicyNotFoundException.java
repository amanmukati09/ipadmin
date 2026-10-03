package com.insurancepolicyadmin.exception;

/**
 * Thrown when a requested Policy identifier or Policy number does not exist.
 * Validates: Requirements 7.3, 8.3, 8.4, 8.9, 9.2, 13.8.
 */
public class PolicyNotFoundException extends ApplicationException {

    public PolicyNotFoundException(String message) {
        super("POLICY_NOT_FOUND", 404, message);
    }

    public PolicyNotFoundException(Long policyId) {
        super("POLICY_NOT_FOUND", 404, "Policy not found: " + policyId);
    }
}
