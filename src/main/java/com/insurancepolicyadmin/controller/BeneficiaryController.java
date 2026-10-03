package com.insurancepolicyadmin.controller;

import com.insurancepolicyadmin.dto.request.BeneficiaryRequest;
import com.insurancepolicyadmin.dto.response.BeneficiaryResponse;
import com.insurancepolicyadmin.service.BeneficiaryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for Beneficiary management (design.md REST API table, endpoints 23-26).
 *
 * <p>Endpoints:</p>
 * <ul>
 *   <li>{@code POST   /api/policies/{policyId}/beneficiaries} - create a Beneficiary (endpoint 23)</li>
 *   <li>{@code GET    /api/policies/{policyId}/beneficiaries} - list a Policy's Beneficiaries (endpoint 24)</li>
 *   <li>{@code PUT    /api/beneficiaries/{beneficiaryId}} - update a Beneficiary (endpoint 25)</li>
 *   <li>{@code DELETE /api/beneficiaries/{beneficiaryId}} - delete a Beneficiary (endpoint 26)</li>
 * </ul>
 *
 * <p>Create/list are nested under the owning Policy; update/delete address the Beneficiary
 * directly. No class-level base path is declared because the two resource shapes differ.</p>
 *
 * <p>This is a thin controller: it delegates all business logic (allocation-total enforcement,
 * existence checks) to {@link BeneficiaryService}.</p>
 */
@RestController
public class BeneficiaryController {

    private final BeneficiaryService beneficiaryService;

    public BeneficiaryController(BeneficiaryService beneficiaryService) {
        this.beneficiaryService = beneficiaryService;
    }

    /**
     * Creates a Beneficiary linked to a Policy (design.md endpoint 23).
     *
     * @param policyId the owning Policy identifier
     * @param request  the Beneficiary field values
     * @return 201 with the created Beneficiary
     */
    @PostMapping("/policies/{policyId}/beneficiaries")
    public ResponseEntity<BeneficiaryResponse> create(@PathVariable Long policyId,
                                                      @RequestBody BeneficiaryRequest request) {
        BeneficiaryResponse response = beneficiaryService.create(policyId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Lists all Beneficiaries belonging to a Policy (design.md endpoint 24).
     *
     * @param policyId the owning Policy identifier
     * @return 200 with the Policy's Beneficiaries (possibly empty)
     */
    @GetMapping("/policies/{policyId}/beneficiaries")
    public ResponseEntity<List<BeneficiaryResponse>> listByPolicy(@PathVariable Long policyId) {
        return ResponseEntity.ok(beneficiaryService.listByPolicy(policyId));
    }

    /**
     * Updates an existing Beneficiary (design.md endpoint 25).
     *
     * @param beneficiaryId the Beneficiary identifier
     * @param request       the updated Beneficiary field values
     * @return 200 with the updated Beneficiary
     */
    @PutMapping("/beneficiaries/{beneficiaryId}")
    public ResponseEntity<BeneficiaryResponse> update(@PathVariable Long beneficiaryId,
                                                      @RequestBody BeneficiaryRequest request) {
        return ResponseEntity.ok(beneficiaryService.update(beneficiaryId, request));
    }

    /**
     * Deletes an existing Beneficiary (design.md endpoint 26).
     *
     * @param beneficiaryId the Beneficiary identifier
     * @return 204 with no content
     */
    @DeleteMapping("/beneficiaries/{beneficiaryId}")
    public ResponseEntity<Void> delete(@PathVariable Long beneficiaryId) {
        beneficiaryService.delete(beneficiaryId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
