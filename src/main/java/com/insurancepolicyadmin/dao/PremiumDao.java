package com.insurancepolicyadmin.dao;

import com.insurancepolicyadmin.model.plain.Installment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Technology-neutral DAO contract for Premium_Schedule and Installment persistence.
 *
 * <p>Implemented by {@code JdbcPremiumDao} using {@code SimpleJdbcCall}/
 * {@code CallableStatement} through {@code JdbcTemplate} exclusively, invoking stored
 * procedures. This interface SHALL NOT reference Hibernate, JPA, {@code JdbcTemplate},
 * or any other persistence-technology-specific type in its method signatures.</p>
 */
public interface PremiumDao {

    /**
     * Generates a Policy's Premium_Schedule and Installments for the given billing
     * frequency, total premium amount, and term start date.
     *
     * @param policyId        the Policy identifier
     * @param totalPremium    the Policy's total premium amount
     * @param billingFrequency the billing frequency (monthly, quarterly, semi_annual, annual)
     * @param termStartDate   the Policy's term start date, used as the first Installment's due date
     */
    void generateInstallments(Long policyId, BigDecimal totalPremium, String billingFrequency,
                               LocalDate termStartDate);

    /**
     * Retrieves the Installment records of a Policy's Premium_Schedule, ordered by due
     * date ascending.
     *
     * @param policyId the Policy identifier
     * @return the matching Installment records, or an empty list if none exist
     */
    List<Installment> findScheduleByPolicyId(Long policyId);
}
