package com.insurancepolicyadmin.dao;

import com.insurancepolicyadmin.model.plain.Coverage;
import com.insurancepolicyadmin.model.plain.Policy;
import com.insurancepolicyadmin.model.plain.PolicyStatusHistory;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Technology-neutral DAO contract for {@link Policy} persistence and lifecycle operations.
 *
 * <p>Implemented by {@code JdbcPolicyDao} using {@code SimpleJdbcCall}/{@code CallableStatement}
 * through {@code JdbcTemplate} exclusively, invoking stored procedures. This interface SHALL NOT
 * reference Hibernate, JPA, {@code JdbcTemplate}, or any other persistence-technology-specific
 * type in its method signatures.</p>
 */
public interface PolicyDao {

    /**
     * Creates a new Policy with an initial status of {@code quoted}, together with its
     * associated Coverage records.
     *
     * @param customerId     the owning Customer identifier
     * @param agentId        the servicing Agent identifier
     * @param termStartDate  the Policy term start date
     * @param termEndDate    the Policy term end date
     * @param billingFrequency the billing frequency (monthly, quarterly, semi_annual, annual)
     * @param totalPremium   the Policy's total premium amount
     * @param coverages      the Coverage definitions to associate with the new Policy (1 to 50)
     * @return the generated Policy identifier
     */
    Long create(Long customerId, Long agentId, LocalDate termStartDate, LocalDate termEndDate,
                String billingFrequency, java.math.BigDecimal totalPremium, List<Coverage> coverages);

    /**
     * Retrieves a Policy by identifier.
     *
     * @param policyId the Policy identifier
     * @return an {@link Optional} containing the Policy if found, otherwise empty
     */
    Optional<Policy> findById(Long policyId);

    /**
     * Retrieves a Policy by its unique Policy number.
     *
     * @param policyNumber the Policy number
     * @return an {@link Optional} containing the Policy if found, otherwise empty
     */
    Optional<Policy> findByPolicyNumber(String policyNumber);

    /**
     * Searches Policy records by any combination of the supplied criteria, combined with
     * AND semantics; any {@code null} criterion is excluded from the filter entirely.
     *
     * @param customerId   the Customer identifier criterion, or {@code null} to omit it
     * @param policyNumber the Policy number criterion, or {@code null} to omit it
     * @param policyStatus the Policy status criterion, or {@code null} to omit it
     * @param agentId      the Agent identifier criterion, or {@code null} to omit it
     * @param page         the zero-based page number to retrieve
     * @param size         the maximum number of records to return (up to 100)
     * @return the matching Policy records for the requested page
     */
    List<Policy> search(Long customerId, String policyNumber, String policyStatus, Long agentId,
                         int page, int size);

    /**
     * Validates and executes a Policy status transition, inserting a corresponding
     * Policy_Status_History record within the same stored procedure invocation.
     *
     * @param policyId  the Policy identifier
     * @param newStatus the requested target status
     * @return a status code: {@code 0} on success, {@code 1} if the transition is not permitted,
     *         {@code 2} if the Policy identifier does not exist
     */
    int transitionStatus(Long policyId, String newStatus);

    /**
     * Retrieves the Policy_Status_History records for a Policy, ordered by transition
     * timestamp ascending.
     *
     * @param policyId the Policy identifier
     * @return the matching Policy_Status_History records, or an empty list if none exist
     */
    List<PolicyStatusHistory> findStatusHistory(Long policyId);

    /**
     * Inserts the initial Policy_Status_History row for a newly created Policy, recording the
     * initial {@code quoted} status with a {@code null} previous status (Requirement 7.2).
     *
     * <p>Invoked by the service layer immediately after {@link #create} within the same
     * transaction, since the Policy-creation stored procedure does not itself write the initial
     * history row.</p>
     *
     * @param policyId the Policy identifier whose initial {@code quoted} history row to insert
     */
    void insertInitialStatusHistory(Long policyId);

    /**
     * Records the approving Underwriter identifier on an existing Policy record (Requirement 5.3),
     * preserving the Policy's other mutable fields.
     *
     * @param policyId      the Policy identifier
     * @param underwriterId the approving Underwriter identifier to record
     */
    void recordUnderwriter(Long policyId, Long underwriterId);
}
