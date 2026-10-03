package com.insurancepolicyadmin.service;

import com.insurancepolicyadmin.dto.request.BeneficiaryRequest;
import com.insurancepolicyadmin.dto.response.BeneficiaryResponse;

import java.util.List;

/**
 * Service-layer contract for Beneficiary management (Requirement 6).
 *
 * <p>Implemented by {@code BeneficiaryServiceImpl}, which delegates persistence to the
 * Hibernate-backed {@code BeneficiaryDao} and consults {@code CustomerSegmentRouter} for
 * segment-based allocation-validation strictness. Field validation uses
 * {@code util.ValidationUtils}-style checks; allocation totals are enforced never to exceed
 * 100 percent per Policy.</p>
 */
public interface BeneficiaryService {

    /**
     * Creates a new Beneficiary linked to an existing Policy.
     *
     * @param policyId the owning Policy identifier (must exist)
     * @param request  the Beneficiary field values
     * @return the created Beneficiary as a response DTO (including the generated identifier)
     */
    BeneficiaryResponse create(Long policyId, BeneficiaryRequest request);

    /**
     * Updates an existing Beneficiary's name, relationship type, and allocation percentage.
     *
     * @param beneficiaryId the Beneficiary identifier (must exist)
     * @param request       the updated Beneficiary field values
     * @return the updated Beneficiary as a response DTO
     */
    BeneficiaryResponse update(Long beneficiaryId, BeneficiaryRequest request);

    /**
     * Lists all Beneficiaries belonging to a Policy.
     *
     * @param policyId the owning Policy identifier (must exist)
     * @return the matching Beneficiaries, or an empty list if the Policy has none
     */
    List<BeneficiaryResponse> listByPolicy(Long policyId);

    /**
     * Deletes an existing Beneficiary.
     *
     * @param beneficiaryId the Beneficiary identifier (must exist)
     */
    void delete(Long beneficiaryId);
}
