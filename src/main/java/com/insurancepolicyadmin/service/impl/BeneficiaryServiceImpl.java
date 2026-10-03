package com.insurancepolicyadmin.service.impl;

import com.insurancepolicyadmin.dao.BeneficiaryDao;
import com.insurancepolicyadmin.dao.CustomerDao;
import com.insurancepolicyadmin.dao.PolicyDao;
import com.insurancepolicyadmin.dto.request.BeneficiaryRequest;
import com.insurancepolicyadmin.dto.response.BeneficiaryResponse;
import com.insurancepolicyadmin.exception.AllocationExceedsLimitException;
import com.insurancepolicyadmin.exception.BeneficiaryNotFoundException;
import com.insurancepolicyadmin.exception.FieldError;
import com.insurancepolicyadmin.exception.PolicyNotFoundException;
import com.insurancepolicyadmin.exception.ValidationException;
import com.insurancepolicyadmin.model.entity.Beneficiary;
import com.insurancepolicyadmin.model.entity.Customer;
import com.insurancepolicyadmin.model.plain.Policy;
import com.insurancepolicyadmin.service.BeneficiaryService;
import com.insurancepolicyadmin.util.CustomerSegmentRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Beneficiary management service (Requirement 6).
 *
 * <p>Delegates all Beneficiary persistence to the Hibernate-backed {@link BeneficiaryDao}
 * and consults {@link CustomerSegmentRouter} to vary allocation-validation strictness by the
 * owning Customer's segment code. Policy existence is confirmed via {@link PolicyDao} (whose
 * underlying persistence is JDBC/stored-procedure based) purely as a read-only existence check,
 * and the Customer's segment code is read via {@link CustomerDao}.</p>
 *
 * <p>Transactions are bound to the Hibernate transaction manager, matching the DAO that owns
 * the Beneficiary writes.</p>
 */
@Service
@Transactional("chainedTransactionManager")
public class BeneficiaryServiceImpl implements BeneficiaryService {

    private static final Logger LOG = LoggerFactory.getLogger(BeneficiaryServiceImpl.class);

    private static final BigDecimal MIN_ALLOCATION = new BigDecimal("0.01");
    private static final BigDecimal MAX_ALLOCATION = new BigDecimal("100.00");
    private static final List<String> ALLOWED_RELATIONSHIP_TYPES =
            Arrays.asList("Spouse", "Child", "Parent", "Sibling", "Other");

    private final BeneficiaryDao beneficiaryDao;
    private final PolicyDao policyDao;
    private final CustomerDao customerDao;

    // INTENTIONAL TENANT BRANCHING: CustomerSegmentRouter is a plain legacy utility
    // (Requirement 16.2), instantiated directly rather than injected as a Spring bean,
    // consistent with how it is written and how ValidationUtils is used.
    private final CustomerSegmentRouter customerSegmentRouter = new CustomerSegmentRouter();

    @Autowired
    public BeneficiaryServiceImpl(BeneficiaryDao beneficiaryDao,
                                  PolicyDao policyDao,
                                  CustomerDao customerDao) {
        this.beneficiaryDao = beneficiaryDao;
        this.policyDao = policyDao;
        this.customerDao = customerDao;
    }

    @Override
    public BeneficiaryResponse create(Long policyId, BeneficiaryRequest request) {
        LOG.info("Creating Beneficiary for Policy {}", policyId);
        Policy policy = requirePolicy(policyId);
        validateFields(request);

        // Allocation-sum check against 100% (Req 6.4): existing total + new allocation.
        BigDecimal existingTotal = safeSum(beneficiaryDao.sumAllocationByPolicyId(policyId));
        BigDecimal attemptedTotal = existingTotal.add(request.getAllocationPercentage());
        enforceAllocationLimit(policyId, policy, attemptedTotal);

        Beneficiary beneficiary = new Beneficiary();
        beneficiary.setPolicyId(policyId);
        beneficiary.setName(request.getName().trim());
        beneficiary.setRelationshipType(request.getRelationshipType());
        beneficiary.setAllocationPercentage(request.getAllocationPercentage());

        Long generatedId = beneficiaryDao.create(beneficiary);
        beneficiary.setBeneficiaryId(generatedId);
        LOG.info("Created Beneficiary {} for Policy {}", generatedId, policyId);
        return toResponse(beneficiary);
    }

    @Override
    public BeneficiaryResponse update(Long beneficiaryId, BeneficiaryRequest request) {
        LOG.info("Updating Beneficiary {}", beneficiaryId);
        Beneficiary existing = requireBeneficiary(beneficiaryId);
        validateFields(request);

        Long policyId = existing.getPolicyId();
        Policy policy = requirePolicy(policyId);

        // Allocation-sum re-check excluding the Beneficiary being updated (Req 6.4):
        // existing total - this Beneficiary's current allocation + requested allocation.
        BigDecimal existingTotal = safeSum(beneficiaryDao.sumAllocationByPolicyId(policyId));
        BigDecimal totalExcludingThis = existingTotal.subtract(safeSum(existing.getAllocationPercentage()));
        BigDecimal attemptedTotal = totalExcludingThis.add(request.getAllocationPercentage());
        enforceAllocationLimit(policyId, policy, attemptedTotal);

        existing.setName(request.getName().trim());
        existing.setRelationshipType(request.getRelationshipType());
        existing.setAllocationPercentage(request.getAllocationPercentage());

        beneficiaryDao.update(existing);
        LOG.info("Updated Beneficiary {} on Policy {}", beneficiaryId, policyId);
        return toResponse(existing);
    }

    @Override
    @Transactional("chainedTransactionManager")
    public List<BeneficiaryResponse> listByPolicy(Long policyId) {
        requirePolicy(policyId);
        List<Beneficiary> beneficiaries = beneficiaryDao.findAllByPolicyId(policyId);
        List<BeneficiaryResponse> responses = new ArrayList<>();
        if (beneficiaries != null) {
            for (Beneficiary beneficiary : beneficiaries) {
                responses.add(toResponse(beneficiary));
            }
        }
        // Empty list on no results is not an error (Req 6.6).
        return responses;
    }

    @Override
    public void delete(Long beneficiaryId) {
        LOG.info("Deleting Beneficiary {}", beneficiaryId);
        requireBeneficiary(beneficiaryId);
        beneficiaryDao.delete(beneficiaryId);
        LOG.info("Deleted Beneficiary {}", beneficiaryId);
    }

    // ---- helpers --------------------------------------------------------

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

    private Beneficiary requireBeneficiary(Long beneficiaryId) {
        if (beneficiaryId == null) {
            throw new BeneficiaryNotFoundException("Beneficiary identifier is required");
        }
        Optional<Beneficiary> beneficiary = beneficiaryDao.findById(beneficiaryId);
        if (!beneficiary.isPresent()) {
            throw new BeneficiaryNotFoundException(beneficiaryId);
        }
        return beneficiary.get();
    }

    private void validateFields(BeneficiaryRequest request) {
        if (request == null) {
            throw new ValidationException("Beneficiary request body is required");
        }
        List<FieldError> errors = new ArrayList<>();

        String name = request.getName();
        if (name == null || name.trim().isEmpty() || name.trim().length() > 100) {
            errors.add(new FieldError("name", "Name must be between 1 and 100 characters"));
        }

        String relationshipType = request.getRelationshipType();
        if (relationshipType == null || !ALLOWED_RELATIONSHIP_TYPES.contains(relationshipType)) {
            errors.add(new FieldError("relationshipType",
                    "Relationship type must be one of " + ALLOWED_RELATIONSHIP_TYPES));
        }

        BigDecimal allocation = request.getAllocationPercentage();
        if (allocation == null) {
            errors.add(new FieldError("allocationPercentage", "Allocation percentage is required"));
        } else {
            if (allocation.scale() > 2) {
                errors.add(new FieldError("allocationPercentage",
                        "Allocation percentage must have at most 2 decimal places"));
            }
            if (allocation.compareTo(MIN_ALLOCATION) < 0 || allocation.compareTo(MAX_ALLOCATION) > 0) {
                errors.add(new FieldError("allocationPercentage",
                        "Allocation percentage must be between 0.01 and 100.00"));
            }
        }

        if (!errors.isEmpty()) {
            throw new ValidationException("Beneficiary validation failed", errors);
        }
    }

    /**
     * Enforces the 100% allocation ceiling (Req 6.4): an attempted total that would exceed
     * 100.00% is rejected, while a total up to and including exactly 100.00% is permitted.
     *
     * <p>The owning Customer's segment code selects validation strictness via
     * {@link CustomerSegmentRouter}. Under strict validation (HIGH_RISK) the attempted total
     * is compared at its exact 2-decimal scale, so any fractional overflow beyond 100.00%
     * is rejected; under non-strict validation the comparison tolerates a sub-cent rounding
     * remainder by first rounding the attempted total to 2 decimal places. In both cases a
     * genuine 100.00% allocation remains valid, honoring Requirement 6.4.</p>
     */
    private void enforceAllocationLimit(Long policyId, Policy policy, BigDecimal attemptedTotal) {
        String segmentCode = resolveSegmentCode(policy);
        boolean strict = customerSegmentRouter.isStrictAllocationValidation(segmentCode);

        BigDecimal comparable = strict
                ? attemptedTotal
                : attemptedTotal.setScale(2, java.math.RoundingMode.HALF_UP);

        if (comparable.compareTo(MAX_ALLOCATION) > 0) {
            throw new AllocationExceedsLimitException(policyId, attemptedTotal);
        }
    }

    private String resolveSegmentCode(Policy policy) {
        if (policy == null || policy.getCustomerId() == null) {
            return null;
        }
        Optional<Customer> customer = customerDao.findById(policy.getCustomerId());
        return customer.map(Customer::getSegmentCode).orElse(null);
    }

    private static BigDecimal safeSum(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private static BeneficiaryResponse toResponse(Beneficiary beneficiary) {
        BeneficiaryResponse response = new BeneficiaryResponse();
        response.setBeneficiaryId(beneficiary.getBeneficiaryId());
        response.setPolicyId(beneficiary.getPolicyId());
        response.setName(beneficiary.getName());
        response.setRelationshipType(beneficiary.getRelationshipType());
        response.setAllocationPercentage(beneficiary.getAllocationPercentage());
        return response;
    }
}
