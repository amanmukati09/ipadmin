package com.insurancepolicyadmin.controller;

import com.insurancepolicyadmin.dto.request.EndorsementRequest;
import com.insurancepolicyadmin.dto.response.EndorsementResponse;
import com.insurancepolicyadmin.service.EndorsementService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for mid-term Endorsement application (design.md REST API table, endpoint 7).
 *
 * <p>Endpoint:</p>
 * <ul>
 *   <li>{@code POST /api/policies/{policyId}/endorsements} - apply an Endorsement (endpoint 7)</li>
 * </ul>
 *
 * <p>This is a thin controller: it delegates all business logic (endorsable-status check,
 * effective-date validation, description-length validation, prorated premium adjustment,
 * schedule regeneration) to {@link EndorsementService}.</p>
 */
@RestController
@RequestMapping("/policies/{policyId}/endorsements")
public class EndorsementController {

    private final EndorsementService endorsementService;

    public EndorsementController(EndorsementService endorsementService) {
        this.endorsementService = endorsementService;
    }

    /**
     * Applies a mid-term Endorsement to a Policy (design.md endpoint 7).
     *
     * @param policyId the Policy identifier the Endorsement is applied against
     * @param request  the Endorsement request (description, effective date, coverage changes)
     * @return 201 with the created Endorsement and the Policy's updated state
     */
    @PostMapping
    public ResponseEntity<EndorsementResponse> applyEndorsement(@PathVariable Long policyId,
                                                                @RequestBody EndorsementRequest request) {
        EndorsementResponse response = endorsementService.applyEndorsement(policyId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
