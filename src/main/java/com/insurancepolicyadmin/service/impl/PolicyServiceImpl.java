package com.insurancepolicyadmin.service.impl;

import com.insurancepolicyadmin.dao.CustomerDao;
import com.insurancepolicyadmin.dao.PolicyDao;
import com.insurancepolicyadmin.dao.UnderwriterDao;
import com.insurancepolicyadmin.dto.request.PolicyIssueRequest;
import com.insurancepolicyadmin.dto.request.PolicySearchCriteria;
import com.insurancepolicyadmin.dto.response.PolicyResponse;
import com.insurancepolicyadmin.dto.response.PolicySearchResponse;
import com.insurancepolicyadmin.dto.response.PolicyStatusHistoryResponse;
import com.insurancepolicyadmin.exception.AgentNotFoundException;
import com.insurancepolicyadmin.exception.CustomerNotFoundException;
import com.insurancepolicyadmin.exception.InvalidStatusTransitionException;
import com.insurancepolicyadmin.exception.PolicyNotFoundException;
import com.insurancepolicyadmin.exception.ScheduleGenerationException;
import com.insurancepolicyadmin.exception.UnderwriterNotFoundException;
import com.insurancepolicyadmin.exception.ValidationException;
import com.insurancepolicyadmin.model.plain.Coverage;
import com.insurancepolicyadmin.model.plain.Policy;
import com.insurancepolicyadmin.model.plain.PolicyStatusHistory;
import com.insurancepolicyadmin.service.PolicyService;
import com.insurancepolicyadmin.service.PremiumService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Default {@link PolicyService} implementation.
 *
 * <p>Business rules (coverage-count validation, Customer/Agent existence, Underwriter active-status,
 * status eligibility) live here; the Policy lifecycle persistence is delegated to {@link PolicyDao}
 * (its {@code JdbcPolicyDao} implementation invokes MySQL stored procedures via Spring JDBC).
 * Customer existence is read through {@link CustomerDao} (Hibernate-backed) and Underwriter
 * active-status through {@link UnderwriterDao}; Premium_Schedule generation during binding is
 * delegated to {@link PremiumService}.</p>
 *
 * <p>Mutating methods run under the Spring-JDBC-backed {@code DataSourceTransactionManager}
 * (qualifier {@code jdbcTransactionManager}). Reading Customer existence (a Hibernate-backed DAO)
 * inside a JDBC-transaction method is an intentional, acceptable existence check for this
 * mixed-persistence fixture.</p>
 *
 * <p>Issuance (Requirements 7.1&ndash;7.4) and binding (Requirements 5.2&ndash;5.4, 7.6&ndash;7.8)
 * are implemented in Task 15.1. The generic status-transition entry point
 * ({@link #transitionStatus(Long, String)}) validates the {@code (current, target)} pair against the
 * six permitted transitions and delegates the atomic update to {@link PolicyDao#transitionStatus}.
 * The status-history read ({@link #getStatusHistory(Long)}) and paged search
 * ({@link #search(PolicySearchCriteria)}) back {@code PolicyController} endpoints 6 and 11.</p>
 */
@Service
public class PolicyServiceImpl implements PolicyService {

    private static final Logger LOG = LoggerFactory.getLogger(PolicyServiceImpl.class);

    private static final int MIN_COVERAGES = 1;
    private static final int MAX_COVERAGES = 50;
    private static final String STATUS_QUOTED = "quoted";
    private static final String STATUS_BOUND = "bound";

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    /**
     * The six permitted Policy_Status transitions (Requirement 8.2), each encoded as
     * {@code "current->target"}. Any pair absent from this set is rejected.
     */
    private static final Set<String> PERMITTED_TRANSITIONS = new HashSet<String>(Arrays.asList(
            "quoted->bound",
            "bound->active",
            "active->lapsed",
            "active->cancelled",
            "active->renewed",
            "lapsed->cancelled"));

    private final PolicyDao policyDao;
    private final CustomerDao customerDao;
    private final UnderwriterDao underwriterDao;
    private final PremiumService premiumService;

    @Autowired
    public PolicyServiceImpl(PolicyDao policyDao, CustomerDao customerDao,
                             UnderwriterDao underwriterDao, PremiumService premiumService) {
        this.policyDao = policyDao;
        this.customerDao = customerDao;
        this.underwriterDao = underwriterDao;
        this.premiumService = premiumService;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Validates the coverage count (1&ndash;50, Requirement 7.4) and the existence of the
     * referenced Customer (Requirement 7.3) and Agent (Requirement 7.3) before delegating creation
     * to {@link PolicyDao#create}. The DAO creates the Policy with initial {@code quoted} status
     * (Requirement 7.1); this method then ensures the corresponding initial Policy_Status_History
     * row (previous = {@code null}, new = {@code quoted}) exists via
     * {@link PolicyDao#insertInitialStatusHistory(Long)} (Requirement 7.2). Both run under one
     * JDBC transaction so the Policy and its initial history row commit atomically.</p>
     */
    @Override
    @Transactional("chainedTransactionManager")
    public PolicyResponse issuePolicy(PolicyIssueRequest request) {
        if (request == null) {
            throw new ValidationException("Policy issuance request must not be null.");
        }
        List<PolicyIssueRequest.CoverageItem> items = request.getCoverages();
        int coverageCount = items == null ? 0 : items.size();
        if (coverageCount < MIN_COVERAGES || coverageCount > MAX_COVERAGES) {
            throw new ValidationException(
                    "A Policy issuance request requires between 1 and 50 Coverage definitions; received "
                            + coverageCount + ".");
        }
        Long customerId = request.getCustomerId();
        if (customerId == null || !customerDao.findById(customerId).isPresent()) {
            throw new CustomerNotFoundException(customerId);
        }
        Long agentId = request.getAgentId();
        if (agentId == null) {
            throw new AgentNotFoundException("Agent not found: null");
        }
        // Agent existence is enforced by the policy stored procedure's FK; a null agent is rejected
        // above, and a non-existent agent surfaces as a persistence error. The explicit null check
        // keeps the fast-path validation symmetric with the Customer check (Requirement 7.3).
        List<Coverage> coverages = toCoverages(customerId, items);
        // Persist the submitted total premium (defaulting null to zero) rather than discarding it;
        // a zero premium would later make Premium_Schedule generation fail at bind time.
        BigDecimal totalPremium =
                request.getTotalPremium() != null ? request.getTotalPremium() : BigDecimal.ZERO;
        Long policyId = policyDao.create(customerId, agentId, request.getTermStartDate(),
                request.getTermEndDate(), request.getBillingFrequency(), totalPremium, coverages);
        // Requirement 7.2: ensure an initial Policy_Status_History row (previous = NULL, new = quoted)
        // exists. sp_policy_create does not insert one, so the service records it explicitly within the
        // same JDBC transaction.
        policyDao.insertInitialStatusHistory(policyId);
        LOG.info("Issued policy {} for customer {} / agent {} with {} coverage(s), status quoted",
                policyId, customerId, agentId, coverageCount);
        return toResponse(requirePolicy(policyId));
    }

    /**
     * {@inheritDoc}
     *
     * <p>Requires the Policy to exist and be in {@code quoted} status
     * (Requirement 7.6 &rarr; {@link InvalidStatusTransitionException}), and the Underwriter
     * identifier to be present (Requirement 5.4), to exist, and to be active (Requirement 5.4
     * &rarr; {@link UnderwriterNotFoundException} / {@link ValidationException}). The
     * Premium_Schedule is generated via {@link PremiumService#generateSchedule} and the Policy is
     * transitioned to {@code bound} only afterwards (Requirement 7.7). Because the whole method runs
     * in a single {@code jdbcTransactionManager} transaction, a schedule-generation failure rolls
     * everything back, leaving the Policy {@code quoted} with no partial schedule and no bound
     * history row (Requirement 7.8).</p>
     */
    @Override
    @Transactional("jdbcTransactionManager")
    public PolicyResponse bindPolicy(Long policyId, Long underwriterId) {
        Policy policy = requirePolicy(policyId);
        if (!STATUS_QUOTED.equals(policy.getPolicyStatus())) {
            throw new InvalidStatusTransitionException(
                    "Policy " + policyId + " is not eligible to be bound; its status is '"
                            + policy.getPolicyStatus() + "' (only 'quoted' policies may be bound).");
        }
        // Requirement 5.2 / 5.4: a bind requires a valid, active Underwriter identifier.
        if (underwriterId == null) {
            throw new ValidationException(
                    "A Policy bind request requires an Underwriter identifier; none was supplied.");
        }
        if (!underwriterDao.findById(underwriterId).isPresent()) {
            throw new UnderwriterNotFoundException(underwriterId);
        }
        if (!underwriterDao.isActive(underwriterId)) {
            throw new ValidationException(
                    "Underwriter " + underwriterId + " is not active and cannot approve a bind.");
        }
        // Requirement 7.7 / 7.8: generate the Premium_Schedule first; only transition to bound on
        // success. Any failure here throws and rolls back the whole transaction, leaving the policy
        // quoted with no partial schedule.
        try {
            premiumService.generateSchedule(policyId, policy.getTotalPremium(),
                    policy.getBillingFrequency(), policy.getTermStartDate());
        } catch (ScheduleGenerationException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new ScheduleGenerationException(
                    "Failed to generate the Premium_Schedule for policy " + policyId
                            + " during binding.", ex);
        }
        int statusCode = policyDao.transitionStatus(policyId, STATUS_BOUND);
        if (statusCode != 0) {
            // The transition stored procedure rejected the quoted -> bound move; roll back the schedule
            // generated above by throwing out of the transaction (Requirement 7.8).
            throw new InvalidStatusTransitionException(policyId, STATUS_QUOTED, STATUS_BOUND);
        }
        // Requirement 5.3: record the approving Underwriter on the Policy record.
        policyDao.recordUnderwriter(policyId, underwriterId);
        LOG.info("Bound policy {} with underwriter {}; Premium_Schedule generated and status now bound",
                policyId, underwriterId);
        return toResponse(requirePolicy(policyId));
    }

    /**
     * {@inheritDoc}
     *
     * <p>Loads the Policy (not-found &rarr; {@link PolicyNotFoundException}, Requirement 8.4),
     * validates the {@code (current, target)} pair against {@link #PERMITTED_TRANSITIONS}
     * (Requirements 8.1, 8.2) and, when permitted, delegates the atomic status update plus
     * Policy_Status_History insertion to {@link PolicyDao#transitionStatus} (Requirement 8.5). A
     * disallowed pair is rejected without modifying the Policy's status (Requirement 8.3); a DAO
     * status code of {@code 1} (procedure-level rejection) is likewise surfaced as an
     * {@link InvalidStatusTransitionException} and a status code of {@code 2} as a
     * {@link PolicyNotFoundException}, leaving state unchanged in both cases.</p>
     */
    @Override
    @Transactional("jdbcTransactionManager")
    public PolicyResponse transitionStatus(Long policyId, String newStatus) {
        Policy policy = requirePolicy(policyId);
        if (newStatus == null || newStatus.trim().isEmpty()) {
            throw new ValidationException("A target Policy_Status is required for a status transition.");
        }
        String current = policy.getPolicyStatus();
        String target = newStatus.trim();
        if (!PERMITTED_TRANSITIONS.contains(current + "->" + target)) {
            throw new InvalidStatusTransitionException(policyId, current, target);
        }
        int statusCode = policyDao.transitionStatus(policyId, target);
        if (statusCode == 2) {
            throw new PolicyNotFoundException(policyId);
        }
        if (statusCode != 0) {
            throw new InvalidStatusTransitionException(policyId, current, target);
        }
        LOG.info("Transitioned policy {} from {} to {}", policyId, current, target);
        return toResponse(requirePolicy(policyId));
    }

    /** {@inheritDoc} */
    @Override
    @Transactional("jdbcTransactionManager")
    public PolicyResponse getById(Long policyId) {
        return toResponse(requirePolicy(policyId));
    }

    /** {@inheritDoc} */
    @Override
    @Transactional("jdbcTransactionManager")
    public PolicyResponse getByNumber(String policyNumber) {
        if (policyNumber == null) {
            throw new PolicyNotFoundException("Policy not found: null");
        }
        Optional<Policy> policy = policyDao.findByPolicyNumber(policyNumber);
        if (!policy.isPresent()) {
            throw new PolicyNotFoundException("Policy not found: " + policyNumber);
        }
        return toResponse(policy.get());
    }

    /**
     * {@inheritDoc}
     *
     * <p>Loads the Policy to enforce existence (Requirement 8.4 / endpoint-6 {@code POLICY_NOT_FOUND}
     * contract) before delegating the ordered read to {@link PolicyDao#findStatusHistory(Long)} and
     * mapping each row to a {@link PolicyStatusHistoryResponse}.</p>
     */
    @Override
    @Transactional("jdbcTransactionManager")
    public List<PolicyStatusHistoryResponse> getStatusHistory(Long policyId) {
        // Existence check first so a missing Policy yields POLICY_NOT_FOUND (404) rather than an
        // empty list (design.md REST API table, endpoint 6).
        requirePolicy(policyId);
        List<PolicyStatusHistory> history = policyDao.findStatusHistory(policyId);
        List<PolicyStatusHistoryResponse> responses =
                new ArrayList<PolicyStatusHistoryResponse>(history.size());
        for (PolicyStatusHistory row : history) {
            responses.add(toStatusHistoryResponse(row));
        }
        return responses;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Normalizes the page bounds, delegates the AND-combined filter to
     * {@link PolicyDao#search(Long, String, String, Long, int, int)} and wraps the mapped
     * {@link PolicyResponse} records with pagination metadata in a {@link PolicySearchResponse}.</p>
     */
    @Override
    @Transactional("jdbcTransactionManager")
    public PolicySearchResponse search(PolicySearchCriteria criteria) {
        PolicySearchCriteria effective = criteria != null ? criteria : new PolicySearchCriteria();

        int page = effective.getPage() < 0 ? 0 : effective.getPage();
        int size = effective.getSize();
        if (size <= 0) {
            size = DEFAULT_PAGE_SIZE;
        } else if (size > MAX_PAGE_SIZE) {
            size = MAX_PAGE_SIZE;
        }

        List<Policy> matches = policyDao.search(effective.getCustomerId(),
                effective.getPolicyNumber(), effective.getPolicyStatus(), effective.getAgentId(),
                page, size);

        List<PolicyResponse> policies = new ArrayList<PolicyResponse>(matches.size());
        for (Policy policy : matches) {
            policies.add(toResponse(policy));
        }

        PolicySearchResponse response = new PolicySearchResponse();
        response.setPolicies(policies);
        response.setTotalCount(policies.size());
        response.setPage(page);
        response.setSize(size);
        LOG.info("Policy search returned {} record(s) for page {} (size {})",
                policies.size(), page, size);
        return response;
    }

    // ------------------------------------------------------------------
    // Internal helpers
    // ------------------------------------------------------------------

    private Policy requirePolicy(Long policyId) {
        if (policyId == null) {
            throw new PolicyNotFoundException("Policy not found: null");
        }
        Optional<Policy> policy = policyDao.findById(policyId);
        if (!policy.isPresent()) {
            throw new PolicyNotFoundException(policyId);
        }
        return policy.get();
    }

    private static List<Coverage> toCoverages(Long customerIdUnused,
                                              List<PolicyIssueRequest.CoverageItem> items) {
        List<Coverage> coverages = new ArrayList<Coverage>(items.size());
        for (PolicyIssueRequest.CoverageItem item : items) {
            Coverage coverage = new Coverage();
            coverage.setCoverageTypeId(item.getCoverageTypeId());
            coverage.setCoverageLimit(item.getCoverageLimit());
            coverages.add(coverage);
        }
        return coverages;
    }

    private static PolicyResponse toResponse(Policy policy) {
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

    private static PolicyStatusHistoryResponse toStatusHistoryResponse(PolicyStatusHistory row) {
        PolicyStatusHistoryResponse response = new PolicyStatusHistoryResponse();
        response.setPolicyStatusHistoryId(row.getPolicyStatusHistoryId());
        response.setPolicyId(row.getPolicyId());
        response.setPreviousStatus(row.getPreviousStatus());
        response.setNewStatus(row.getNewStatus());
        response.setTransitionedAt(row.getTransitionedAt());
        return response;
    }
}
