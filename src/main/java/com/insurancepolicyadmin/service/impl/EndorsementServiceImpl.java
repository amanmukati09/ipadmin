package com.insurancepolicyadmin.service.impl;

import com.insurancepolicyadmin.dao.EndorsementDao;
import com.insurancepolicyadmin.dao.PolicyDao;
import com.insurancepolicyadmin.dto.request.EndorsementRequest;
import com.insurancepolicyadmin.dto.response.EndorsementResponse;
import com.insurancepolicyadmin.dto.response.PolicyResponse;
import com.insurancepolicyadmin.exception.PolicyNotEndorsableException;
import com.insurancepolicyadmin.exception.PolicyNotFoundException;
import com.insurancepolicyadmin.exception.ValidationException;
import com.insurancepolicyadmin.model.plain.Policy;
import com.insurancepolicyadmin.service.EndorsementService;
import com.insurancepolicyadmin.service.PremiumService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Default {@link EndorsementService} implementation (Requirement 9).
 *
 * <p>Endorsement persistence and premium proration are delegated to {@link EndorsementDao} (its
 * {@code JdbcEndorsementDao} implementation invokes MySQL stored procedures via Spring JDBC, with the
 * single documented {@code premium_adjustment_history} exception); Policy state is read through
 * {@link PolicyDao}; and the remaining Premium_Schedule Installments are regenerated through
 * {@link EndorsementDao#regenerateRemainingInstallments(Long, java.math.BigDecimal)}. {@link PremiumService}
 * is injected because the Endorsement flow is a sibling of schedule generation and keeps the service
 * wiring aligned with design.md (PremiumService is consumed when a mid-term change requires the
 * Premium_Schedule to be regenerated).</p>
 *
 * <p>{@link #applyEndorsement(Long, EndorsementRequest)} runs under the Spring-JDBC-backed
 * {@code DataSourceTransactionManager} (qualifier {@code jdbcTransactionManager}). Because the premium
 * update + Premium_Adjustment_History insertion ({@link EndorsementDao#applyProrationAndRecordHistory})
 * and the Installment regeneration both participate in this single transaction, a failure in the
 * regeneration step rolls back the premium update and the history row, leaving the Policy's premium,
 * the Premium_Adjustment_History table, and the Premium_Schedule exactly as they were before the request
 * (Requirements 9.7, 9.8, 9.9, 9.10).</p>
 */
@Service
public class EndorsementServiceImpl implements EndorsementService {

    private static final Logger LOG = LoggerFactory.getLogger(EndorsementServiceImpl.class);

    /** The only Policy status in which an Endorsement may be applied (Requirement 9.2). */
    private static final String ACTIVE_STATUS = "active";

    /** Maximum Endorsement description length per the schema and Requirements 9.1/9.4. */
    private static final int MAX_DESCRIPTION_LENGTH = 1000;

    private final EndorsementDao endorsementDao;
    private final PolicyDao policyDao;
    private final PremiumService premiumService;

    @Autowired
    public EndorsementServiceImpl(EndorsementDao endorsementDao,
                                  PolicyDao policyDao,
                                  PremiumService premiumService) {
        this.endorsementDao = endorsementDao;
        this.policyDao = policyDao;
        this.premiumService = premiumService;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Order of operations: confirm the Policy exists (Requirement 9.2 not-found handling) and is in
     * {@code active} status; validate the effective date lies within {@code [termStartDate, termEndDate]}
     * (Requirement 9.3) and the description length is within bounds (Requirement 9.4); compute the signed
     * coverage delta from the requested coverage changes and ask the DAO to prorate it over the days
     * remaining in the term (Requirements 9.5/9.6); create the Endorsement row; apply the adjustment and
     * record the Premium_Adjustment_History row (Requirement 9.7); then regenerate the remaining
     * Installments against the new total premium (Requirement 9.9). All mutating steps share the one
     * transaction opened by this method, so any failure rolls them back together (Requirements 9.8,
     * 9.10).</p>
     */
    @Override
    @Transactional("jdbcTransactionManager")
    public EndorsementResponse applyEndorsement(Long policyId, EndorsementRequest request) {
        LOG.info("Applying Endorsement to Policy {}", policyId);

        Policy policy = requirePolicy(policyId);
        requireEndorsable(policy);
        validateRequest(policy, request);

        BigDecimal coverageDelta = computeCoverageDelta(request);

        // Signed prorated premium adjustment: positive for coverage increase/addition,
        // negative for decrease/removal (Requirements 9.5, 9.6). The DAO re-checks endorsability
        // and throws PolicyNotEndorsableException if the Policy is not active.
        BigDecimal adjustment = endorsementDao.calculateProration(
                policyId, coverageDelta, request.getEffectiveDate());

        Long endorsementId = endorsementDao.create(
                policyId, request.getDescription(), request.getEffectiveDate(), adjustment);
        LOG.info("Created Endorsement {} on Policy {} (adjustment {})", endorsementId, policyId, adjustment);

        // Update policy.total_premium and insert the Premium_Adjustment_History row (Requirement 9.7),
        // joining this method's transaction so it rolls back if the regeneration below fails.
        endorsementDao.applyProrationAndRecordHistory(policyId, endorsementId, adjustment);

        BigDecimal newTotalPremium = safe(policy.getTotalPremium()).add(safe(adjustment));

        // Regenerate the not-yet-due Installments to reflect the adjusted premium, leaving already-paid
        // Installments unchanged (the stored procedure handles that). A failure here rolls back the
        // premium update and the history row above (Requirements 9.9, 9.10).
        endorsementDao.regenerateRemainingInstallments(policyId, newTotalPremium);
        LOG.info("Regenerated remaining Installments for Policy {} at new total premium {}",
                policyId, newTotalPremium);

        Policy updatedPolicy = policyDao.findById(policyId).orElse(policy);
        return toResponse(endorsementId, policyId, request, adjustment, updatedPolicy);
    }

    // ------------------------------------------------------------------
    // Validation helpers
    // ------------------------------------------------------------------

    private Policy requirePolicy(Long policyId) {
        if (policyId == null) {
            throw new PolicyNotFoundException("Policy identifier is required");
        }
        Optional<Policy> policy = policyDao.findById(policyId);
        if (!policy.isPresent()) {
            throw new PolicyNotFoundException(policyId);
        }
        return policy.get();
    }

    private void requireEndorsable(Policy policy) {
        String status = policy.getPolicyStatus();
        if (!ACTIVE_STATUS.equals(status)) {
            throw new PolicyNotEndorsableException(policy.getPolicyId(), String.valueOf(status));
        }
    }

    private void validateRequest(Policy policy, EndorsementRequest request) {
        if (request == null) {
            throw new ValidationException("Endorsement request body is required");
        }

        String description = request.getDescription();
        if (description == null || description.trim().isEmpty()) {
            throw new ValidationException("Endorsement description is required (1 to "
                    + MAX_DESCRIPTION_LENGTH + " characters)");
        }
        if (description.length() > MAX_DESCRIPTION_LENGTH) {
            throw new ValidationException("Endorsement description must not exceed "
                    + MAX_DESCRIPTION_LENGTH + " characters");
        }

        LocalDate effectiveDate = request.getEffectiveDate();
        if (effectiveDate == null) {
            throw new ValidationException("Endorsement effective date is required");
        }
        LocalDate termStart = policy.getTermStartDate();
        LocalDate termEnd = policy.getTermEndDate();
        if ((termStart != null && effectiveDate.isBefore(termStart))
                || (termEnd != null && effectiveDate.isAfter(termEnd))) {
            throw new ValidationException("Endorsement effective date " + effectiveDate
                    + " must fall within the Policy's current term [" + termStart + ", " + termEnd + "]");
        }
    }

    // ------------------------------------------------------------------
    // Coverage-delta derivation
    // ------------------------------------------------------------------

    /**
     * Derives the signed annualized premium impact of the requested coverage changes: an added or
     * increased Coverage contributes a positive amount (its new limit), while a removed Coverage
     * contributes a negative amount (Requirements 9.5, 9.6). The resulting signed delta is what the DAO
     * prorates over the days remaining in the Policy's term. A request with no coverage changes yields a
     * zero delta.
     */
    private static BigDecimal computeCoverageDelta(EndorsementRequest request) {
        BigDecimal delta = BigDecimal.ZERO;
        List<EndorsementRequest.CoverageChange> changes = request.getCoverageChanges();
        if (changes == null) {
            return delta;
        }
        for (EndorsementRequest.CoverageChange change : changes) {
            if (change == null) {
                continue;
            }
            BigDecimal limit = safe(change.getNewCoverageLimit());
            if (change.isRemove()) {
                delta = delta.subtract(limit);
            } else {
                delta = delta.add(limit);
            }
        }
        return delta;
    }

    // ------------------------------------------------------------------
    // Response mapping
    // ------------------------------------------------------------------

    private static EndorsementResponse toResponse(Long endorsementId, Long policyId,
                                                  EndorsementRequest request, BigDecimal adjustment,
                                                  Policy policy) {
        EndorsementResponse response = new EndorsementResponse();
        response.setEndorsementId(endorsementId);
        response.setPolicyId(policyId);
        response.setDescription(request.getDescription());
        response.setEffectiveDate(request.getEffectiveDate());
        response.setPremiumAdjustment(adjustment);
        response.setPolicy(toPolicyResponse(policy));
        return response;
    }

    private static PolicyResponse toPolicyResponse(Policy policy) {
        if (policy == null) {
            return null;
        }
        PolicyResponse response = new PolicyResponse();
        response.setPolicyId(policy.getPolicyId());
        response.setPolicyNumber(policy.getPolicyNumber());
        response.setCustomerId(policy.getCustomerId());
        response.setAgentId(policy.getAgentId());
        response.setUnderwriterId(policy.getUnderwriterId());
        response.setPolicyStatus(policy.getPolicyStatus());
        response.setTermStartDate(policy.getTermStartDate());
        response.setTermEndDate(policy.getTermEndDate());
        response.setBillingFrequency(policy.getBillingFrequency());
        response.setTotalPremium(policy.getTotalPremium());
        response.setCreatedAt(policy.getCreatedAt());
        return response;
    }

    private static BigDecimal safe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
