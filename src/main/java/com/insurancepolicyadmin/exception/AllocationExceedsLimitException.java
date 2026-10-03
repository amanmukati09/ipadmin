package com.insurancepolicyadmin.exception;

/**
 * Thrown when adding or updating a Beneficiary would cause the sum of allocation
 * percentages for a Policy to exceed 100 percent.
 * Validates: Requirements 6.4.
 */
public class AllocationExceedsLimitException extends ApplicationException {

    public AllocationExceedsLimitException(String message) {
        super("ALLOCATION_EXCEEDS_100", 400, message);
    }

    public AllocationExceedsLimitException(Long policyId, java.math.BigDecimal attemptedTotal) {
        super("ALLOCATION_EXCEEDS_100", 400,
                "Beneficiary allocation total " + attemptedTotal + " exceeds 100% for Policy " + policyId);
    }
}
