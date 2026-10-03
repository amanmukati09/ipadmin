package com.insurancepolicyadmin.dao;

import com.insurancepolicyadmin.model.plain.Policy;
import com.insurancepolicyadmin.model.plain.RenewalNotice;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Technology-neutral DAO contract for reporting-style queries and Renewal_Notice persistence.
 *
 * <p>Implemented by {@code JdbcReportDao} using {@code SimpleJdbcCall}/
 * {@code CallableStatement} through {@code JdbcTemplate} exclusively, invoking stored
 * procedures. This interface SHALL NOT reference Hibernate, JPA, {@code JdbcTemplate},
 * or any other persistence-technology-specific type in its method signatures.</p>
 */
public interface ReportDao {

    /**
     * Generates a Renewal_Notice for a Policy if one does not already exist for the
     * Policy's current renewal cycle, persisting the result via a stored procedure.
     *
     * @param policyId    the Policy identifier
     * @param termEndDate the Policy's current term end date, snapshotted onto the notice
     * @return the generated or pre-existing Renewal_Notice record
     */
    RenewalNotice generateRenewalNotice(Long policyId, LocalDate termEndDate);

    /**
     * Retrieves active Policies whose current term end date falls within the given
     * number of days from the current date.
     *
     * @param dayCount the lookahead window in days (1 to 365)
     * @return up to 1000 matching Policy records, or an empty list if none match or if
     *         {@code dayCount} is zero or negative
     */
    List<Policy> findExpiringPolicies(int dayCount);

    /**
     * Retrieves Installments that are unpaid and past their due date, across all Policies.
     *
     * @return up to 1000 matching {@link OverdueInstallment} records, or an empty list if none match
     */
    List<OverdueInstallment> findOverdueInstallments();

    /**
     * Retrieves the Policies attributed to an Agent, including each Policy's current
     * status and current premium amount.
     *
     * @param agentId the Agent identifier
     * @return up to 1000 matching Policy records, or an empty list if the Agent exists but
     *         has zero attributed Policies
     */
    List<Policy> findAgentBookOfBusiness(Long agentId);

    /**
     * A single overdue, unpaid Installment reporting row, together with the identifiers
     * of the Policy and Customer it is associated with.
     *
     * <p>A plain, technology-neutral result holder used solely for reporting purposes.</p>
     */
    class OverdueInstallment {

        private final Long installmentId;
        private final Long policyId;
        private final Long customerId;
        private final LocalDate dueDate;
        private final BigDecimal amountDue;
        private final BigDecimal amountPaid;

        public OverdueInstallment(Long installmentId, Long policyId, Long customerId,
                                   LocalDate dueDate, BigDecimal amountDue, BigDecimal amountPaid) {
            this.installmentId = installmentId;
            this.policyId = policyId;
            this.customerId = customerId;
            this.dueDate = dueDate;
            this.amountDue = amountDue;
            this.amountPaid = amountPaid;
        }

        public Long getInstallmentId() {
            return installmentId;
        }

        public Long getPolicyId() {
            return policyId;
        }

        public Long getCustomerId() {
            return customerId;
        }

        public LocalDate getDueDate() {
            return dueDate;
        }

        public BigDecimal getAmountDue() {
            return amountDue;
        }

        public BigDecimal getAmountPaid() {
            return amountPaid;
        }
    }
}
