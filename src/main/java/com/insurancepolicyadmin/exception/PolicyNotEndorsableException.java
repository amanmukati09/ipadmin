package com.insurancepolicyadmin.exception;

/**
 * Thrown when an Endorsement is requested against a Policy that is not in an endorsable
 * (active) state.
 * Validates: Requirements 9.2.
 */
public class PolicyNotEndorsableException extends ApplicationException {

    public PolicyNotEndorsableException(String message) {
        super("POLICY_NOT_ENDORSABLE", 400, message);
    }

    public PolicyNotEndorsableException(Long policyId, String currentStatus) {
        super("POLICY_NOT_ENDORSABLE", 400,
                "Policy " + policyId + " is not endorsable in status " + currentStatus);
    }
}
