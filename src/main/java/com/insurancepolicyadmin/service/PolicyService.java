package com.insurancepolicyadmin.service;

import com.insurancepolicyadmin.dto.request.PolicyIssueRequest;
import com.insurancepolicyadmin.dto.request.PolicySearchCriteria;
import com.insurancepolicyadmin.dto.response.PolicyResponse;
import com.insurancepolicyadmin.dto.response.PolicySearchResponse;
import com.insurancepolicyadmin.dto.response.PolicyStatusHistoryResponse;

import java.util.List;

/**
 * Service-layer contract for Policy issuance and lifecycle business logic (Requirements 5, 7, 8).
 *
 * <p>Implemented by {@code PolicyServiceImpl}, which delegates persistence to {@code JdbcPolicyDao}
 * (stored procedures via Spring JDBC), reads Customer existence through {@code CustomerDao}
 * (Hibernate-backed) and Underwriter active-status through {@code UnderwriterDao}, and delegates
 * Premium_Schedule generation during binding to {@link PremiumService}.</p>
 *
 * <p>This interface exposes issuance and binding (Task 15.1) plus the status-transition entry point
 * ({@link #transitionStatus(Long, String)}) consumed by {@code EndorsementService},
 * {@code PaymentService} (the 30-day-overdue lapse trigger, Requirement 11.10), and
 * {@code PolicyController}. It also exposes the status-history read
 * ({@link #getStatusHistory(Long)}) and paged search ({@link #search(PolicySearchCriteria)})
 * consumed by {@code PolicyController} (design.md REST API table, endpoints 6 and 11).</p>
 */
public interface PolicyService {

    /**
     * Issues a new Policy for a Customer with an initial Policy_Status of {@code quoted}.
     *
     * <p>Validates the issuance request contains between 1 and 50 Coverage definitions
     * (Requirement 7.4), that the referenced Customer exists (Requirement 7.3) and the referenced
     * Agent exists (Requirement 7.3), then creates the Policy via {@code JdbcPolicyDao} with initial
     * {@code quoted} status (Requirement 7.1) and ensures a corresponding initial
     * Policy_Status_History row (previous = {@code null}, new = {@code quoted}) exists
     * (Requirement 7.2). Issuance runs under a single JDBC-backed transaction so the Policy, its
     * Coverages, and the initial history row commit atomically.</p>
     *
     * @param request the Policy issuance request
     * @return the created Policy as a {@link PolicyResponse}
     */
    PolicyResponse issuePolicy(PolicyIssueRequest request);

    /**
     * Binds a {@code quoted} Policy, recording the approving Underwriter and generating the
     * Policy's Premium_Schedule.
     *
     * <p>Requires the Policy to exist and be in {@code quoted} status (Requirement 7.6), and a
     * valid, active Underwriter identifier (Requirements 5.2, 5.4). The Premium_Schedule is
     * generated via {@link PremiumService} and, only upon successful generation, the Policy
     * transitions to {@code bound} with a corresponding Policy_Status_History row inserted by the
     * underlying stored procedure (Requirement 7.7). The whole operation is atomic: if schedule
     * generation fails, the Policy stays {@code quoted} with no partial schedule and no bound
     * history row (Requirement 7.8).</p>
     *
     * @param policyId      the Policy identifier to bind
     * @param underwriterId the approving Underwriter identifier (must exist and be active)
     * @return the bound Policy as a {@link PolicyResponse}
     */
    PolicyResponse bindPolicy(Long policyId, Long underwriterId);

    /**
     * Validates and executes a Policy_Status transition against the permitted set
     * ({@code quoted -> bound}, {@code bound -> active}, {@code active -> lapsed},
     * {@code active -> cancelled}, {@code active -> renewed}, {@code lapsed -> cancelled}),
     * delegating the atomic status update plus Policy_Status_History insertion to
     * {@code JdbcPolicyDao.transitionStatus(...)} (Requirements 8.1, 8.2, 8.3, 8.4).
     *
     * <p>A transition whose {@code (current, target)} pair is not permitted is rejected without
     * modifying the Policy's status ({@code InvalidStatusTransitionException}); a non-existent
     * Policy identifier yields a {@code PolicyNotFoundException}. This method is the entry point the
     * {@code PaymentService} uses to drive the {@code active -> lapsed} transition when it detects a
     * more-than-30-day-overdue unpaid Installment (Requirement 11.10).</p>
     *
     * @param policyId  the Policy identifier
     * @param newStatus the requested target Policy_Status
     * @return the updated Policy as a {@link PolicyResponse}
     */
    PolicyResponse transitionStatus(Long policyId, String newStatus);

    /**
     * Retrieves a Policy by identifier.
     *
     * @param policyId the Policy identifier
     * @return the Policy as a {@link PolicyResponse}
     * @throws com.insurancepolicyadmin.exception.PolicyNotFoundException if the Policy does not exist
     */
    PolicyResponse getById(Long policyId);

    /**
     * Retrieves a Policy by its unique Policy number.
     *
     * @param policyNumber the Policy number
     * @return the Policy as a {@link PolicyResponse}
     * @throws com.insurancepolicyadmin.exception.PolicyNotFoundException if the Policy does not exist
     */
    PolicyResponse getByNumber(String policyNumber);

    /**
     * Retrieves the chronological Policy_Status_History for a Policy (design.md REST API table,
     * endpoint 6, Requirement 14.6 / 8.5).
     *
     * <p>The Policy must exist; a non-existent identifier yields a
     * {@code PolicyNotFoundException}. The returned list is ordered by transition timestamp
     * ascending (as produced by {@code PolicyDao.findStatusHistory(Long)}) and is empty only for a
     * Policy that somehow has no recorded history.</p>
     *
     * @param policyId the Policy identifier
     * @return the Policy_Status_History records as {@link PolicyStatusHistoryResponse} items
     * @throws com.insurancepolicyadmin.exception.PolicyNotFoundException if the Policy does not exist
     */
    List<PolicyStatusHistoryResponse> getStatusHistory(Long policyId);

    /**
     * Searches Policy records by any combination of the supplied criteria, returning a single page
     * of matching Policies together with pagination metadata (design.md REST API table, endpoint
     * 11, Requirement 14.11).
     *
     * <p>Criteria are combined with AND semantics; any {@code null} criterion is omitted from the
     * filter entirely. Page bounds are normalized (negative page clamped to {@code 0}; size clamped
     * to the inclusive range {@code 1..100}, defaulting to {@code 20} when non-positive) before the
     * query is delegated to {@code PolicyDao.search(...)}.</p>
     *
     * @param criteria the mutable search-criteria bean (customerId, policyNumber, policyStatus,
     *                 agentId, page, size)
     * @return the matching Policies for the requested page as a {@link PolicySearchResponse}
     */
    PolicySearchResponse search(PolicySearchCriteria criteria);
}
