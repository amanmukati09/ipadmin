package com.insurancepolicyadmin.dao;

import com.insurancepolicyadmin.model.entity.Beneficiary;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Technology-neutral DAO contract for {@link Beneficiary} persistence.
 *
 * <p>Implemented by {@code HibernateBeneficiaryDao} using Hibernate session operations
 * exclusively. This interface SHALL NOT reference Hibernate, JPA, {@code JdbcTemplate},
 * or any other persistence-technology-specific type in its method signatures.</p>
 */
public interface BeneficiaryDao {

    /**
     * Persists a new Beneficiary record.
     *
     * @param beneficiary the Beneficiary to create
     * @return the generated Beneficiary identifier
     */
    Long create(Beneficiary beneficiary);

    /**
     * Retrieves a Beneficiary by identifier.
     *
     * @param beneficiaryId the Beneficiary identifier
     * @return an {@link Optional} containing the Beneficiary if found, otherwise empty
     */
    Optional<Beneficiary> findById(Long beneficiaryId);

    /**
     * Retrieves all Beneficiary records associated with a Policy.
     *
     * @param policyId the Policy identifier
     * @return the matching Beneficiary records, or an empty list if none match
     */
    List<Beneficiary> findAllByPolicyId(Long policyId);

    /**
     * Persists changes to an existing Beneficiary record.
     *
     * @param beneficiary the Beneficiary with updated field values
     */
    void update(Beneficiary beneficiary);

    /**
     * Removes a Beneficiary record by identifier.
     *
     * @param beneficiaryId the Beneficiary identifier
     */
    void delete(Long beneficiaryId);

    /**
     * Sums the allocation percentages of all Beneficiary records associated with a Policy.
     *
     * @param policyId the Policy identifier
     * @return the sum of allocation percentages, or {@link BigDecimal#ZERO} if the Policy
     *         has no Beneficiary records
     */
    BigDecimal sumAllocationByPolicyId(Long policyId);
}
