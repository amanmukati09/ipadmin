package com.insurancepolicyadmin.dao;

import com.insurancepolicyadmin.model.plain.Endorsement;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Technology-neutral DAO contract for {@link Endorsement} persistence and premium
 * proration operations.
 *
 * <p>Implemented by {@code JdbcEndorsementDao} using {@code SimpleJdbcCall}/
 * {@code CallableStatement} through {@code JdbcTemplate} exclusively, invoking stored
 * procedures. This interface SHALL NOT reference Hibernate, JPA, {@code JdbcTemplate},
 * or any other persistence-technology-specific type in its method signatures.</p>
 */
public interface EndorsementDao {

    /**
     * Creates a new Endorsement record against a Policy.
     *
     * @param policyId      the Policy identifier
     * @param description   the description of the Coverage or term change (1 to 1000 characters)
     * @param effectiveDate the Endorsement's effective date
     * @param premiumAdjustment the signed premium adjustment amount associated with the Endorsement
     * @return the generated Endorsement identifier
     */
    Long create(Long policyId, String description, LocalDate effectiveDate, BigDecimal premiumAdjustment);

    /**
     * Retrieves an Endorsement by identifier.
     *
     * @param endorsementId the Endorsement identifier
     * @return an {@link Optional} containing the Endorsement if found, otherwise empty
     */
    Optional<Endorsement> findById(Long endorsementId);

    /**
     * Retrieves all Endorsement records associated with a Policy.
     *
     * @param policyId the Policy identifier
     * @return the matching Endorsement records, or an empty list if none exist
     */
    List<Endorsement> findAllByPolicyId(Long policyId);

    /**
     * Calculates the prorated premium adjustment for a coverage change on a Policy, based
     * on the number of days remaining in the Policy's current term relative to the term's
     * total length.
     *
     * @param policyId       the Policy identifier
     * @param coverageDelta  the signed annualized premium impact of the coverage change
     *                       (positive for an increase/addition, negative for a decrease/removal)
     * @param effectiveDate  the Endorsement's effective date
     * @return the signed prorated premium adjustment amount, rounded to 2 decimal places
     */
    BigDecimal calculateProration(Long policyId, BigDecimal coverageDelta, LocalDate effectiveDate);

    /**
     * Applies a prorated premium adjustment to a Policy's current premium amount and
     * records a corresponding Premium_Adjustment_History entry, within a single stored
     * procedure invocation.
     *
     * @param policyId           the Policy identifier
     * @param endorsementId      the Endorsement identifier that caused the adjustment
     * @param adjustmentAmount   the signed premium adjustment amount
     */
    void applyProrationAndRecordHistory(Long policyId, Long endorsementId, BigDecimal adjustmentAmount);

    /**
     * Regenerates the remaining, not-yet-due Installments of a Policy's Premium_Schedule to
     * reflect an adjusted total premium, leaving already-paid Installments unchanged.
     *
     * @param policyId     the Policy identifier
     * @param newTotalPremium the Policy's adjusted total premium amount
     */
    void regenerateRemainingInstallments(Long policyId, BigDecimal newTotalPremium);
}
