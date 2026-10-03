package com.insurancepolicyadmin.exception;

/**
 * Thrown when a requested Policy_Status transition is not in the permitted set.
 * Validates: Requirements 8.3.
 */
public class InvalidStatusTransitionException extends ApplicationException {

    public InvalidStatusTransitionException(String message) {
        super("INVALID_STATUS_TRANSITION", 400, message);
    }

    public InvalidStatusTransitionException(Long policyId, String currentStatus, String targetStatus) {
        super("INVALID_STATUS_TRANSITION", 400,
                "Cannot transition Policy " + policyId + " from " + currentStatus + " to " + targetStatus);
    }
}
