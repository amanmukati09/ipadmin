package com.insurancepolicyadmin.service;

import com.insurancepolicyadmin.dto.request.EndorsementRequest;
import com.insurancepolicyadmin.dto.response.EndorsementResponse;

/**
 * Service-layer contract for mid-term Endorsement application (Requirement 9).
 *
 * <p>Implemented by {@code EndorsementServiceImpl}, which delegates Endorsement persistence and
 * premium proration to {@code JdbcEndorsementDao}, reads Policy state through {@code PolicyDao}, and
 * regenerates the remaining Premium_Schedule Installments via {@code PremiumService}. All business
 * validation (endorsable-status, effective-date-within-term, description length) lives in the
 * implementation; the signed prorated premium adjustment, the premium update plus
 * Premium_Adjustment_History insertion, and the Installment regeneration are applied atomically so a
 * failure in any step rolls the whole Endorsement back (Requirements 9.1 through 9.11).</p>
 */
public interface EndorsementService {

    /**
     * Applies a mid-term Endorsement to an active Policy.
     *
     * <p>Rejects the request when the Policy does not exist ({@code PolicyNotFoundException}), when the
     * Policy is not in {@code active} status ({@code PolicyNotEndorsableException}, Requirement 9.2),
     * when the effective date falls outside the Policy's current term ({@code ValidationException},
     * Requirement 9.3), or when the description is empty or exceeds 1000 characters
     * ({@code ValidationException}, Requirement 9.4). On success it calculates the signed prorated
     * premium adjustment from the requested coverage changes (positive for a coverage increase/addition,
     * negative for a decrease/removal, Requirements 9.5/9.6), creates the Endorsement record, applies the
     * adjustment to {@code policy.total_premium} while inserting a Premium_Adjustment_History row
     * (Requirement 9.7), and regenerates the remaining, not-yet-due Installments to reflect the adjusted
     * premium while leaving already-paid Installments unchanged (Requirement 9.9). The premium update,
     * history insertion, and schedule regeneration run within a single transaction, so a regeneration
     * failure rolls back the premium update and history row (Requirements 9.8, 9.10).</p>
     *
     * @param policyId the Policy identifier the Endorsement is applied against
     * @param request  the Endorsement request (description, effective date, coverage changes)
     * @return the created Endorsement together with the Policy's updated state
     */
    EndorsementResponse applyEndorsement(Long policyId, EndorsementRequest request);
}
