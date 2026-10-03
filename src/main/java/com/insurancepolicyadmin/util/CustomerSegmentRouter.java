package com.insurancepolicyadmin.util;

/**
 * Routes beneficiary-allocation validation strictness and payment grace-period
 * rules by {@code Customer.segmentCode} ({@code STANDARD}/{@code PREFERRED}/
 * {@code HIGH_RISK}).
 *
 * <p><strong>INTENTIONAL LEGACY SMELL:</strong> this class branches behavior
 * directly on the customer's segment code via if/else-if chains rather than
 * through a configuration table, strategy objects, or a Spring-managed
 * per-segment bean. It is called directly as
 * {@code CustomerSegmentRouter.getPaymentGracePeriodDays(...)} /
 * {@code isStrictAllocationValidation(...)} from {@code BeneficiaryService}
 * and {@code PaymentService}. This is intentional per the legacy-smell
 * catalog (Requirement 16.2) and is not to be refactored into a
 * configuration-driven or polymorphic design.</p>
 */
public class CustomerSegmentRouter {

    /**
     * Returns the payment grace-period length, in days, applicable to the
     * given customer segment code. {@code HIGH_RISK} customers get a shorter
     * grace period; {@code PREFERRED} customers get a longer one;
     * {@code STANDARD} and any unrecognized/null segment code default to the
     * standard grace period.
     */
    public int getPaymentGracePeriodDays(String segmentCode) {
        // INTENTIONAL TENANT BRANCHING
        if ("HIGH_RISK".equals(segmentCode)) {
            return 5;
        } else if ("PREFERRED".equals(segmentCode)) {
            return 15;
        } else {
            return 10; // STANDARD and default
        }
    }

    /**
     * Indicates whether strict beneficiary-allocation validation applies to
     * the given customer segment code. {@code HIGH_RISK} customers are
     * subject to strict validation; all other segments (including
     * {@code STANDARD} and {@code PREFERRED}) are not.
     */
    public boolean isStrictAllocationValidation(String segmentCode) {
        // INTENTIONAL TENANT BRANCHING
        if ("HIGH_RISK".equals(segmentCode)) {
            return true;
        } else if ("PREFERRED".equals(segmentCode)) {
            return false;
        } else {
            return false; // STANDARD and default
        }
    }
}
