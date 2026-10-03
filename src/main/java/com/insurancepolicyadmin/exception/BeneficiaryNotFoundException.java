package com.insurancepolicyadmin.exception;

/**
 * Thrown when a requested Beneficiary identifier does not exist.
 * Validates: Requirements 6.2.
 */
public class BeneficiaryNotFoundException extends ApplicationException {

    public BeneficiaryNotFoundException(String message) {
        super("BENEFICIARY_NOT_FOUND", 404, message);
    }

    public BeneficiaryNotFoundException(Long beneficiaryId) {
        super("BENEFICIARY_NOT_FOUND", 404, "Beneficiary not found: " + beneficiaryId);
    }
}
