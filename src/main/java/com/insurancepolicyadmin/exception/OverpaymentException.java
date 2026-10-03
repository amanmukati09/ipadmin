package com.insurancepolicyadmin.exception;

/**
 * Thrown when a Payment amount exceeds the Policy's total remaining unpaid balance,
 * including the case where the remaining balance is zero.
 * Validates: Requirements 11.7.
 */
public class OverpaymentException extends ApplicationException {

    public OverpaymentException(String message) {
        super("OVERPAYMENT", 400, message);
    }

    public OverpaymentException(Long policyId, java.math.BigDecimal amount, java.math.BigDecimal remainingBalance) {
        super("OVERPAYMENT", 400,
                "Payment amount " + amount + " exceeds remaining balance " + remainingBalance
                        + " for Policy " + policyId);
    }
}
