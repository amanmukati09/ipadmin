package com.insurancepolicyadmin.controller;

import com.insurancepolicyadmin.dto.request.PolicyIssueRequest;
import com.insurancepolicyadmin.dto.request.PolicySearchCriteria;
import com.insurancepolicyadmin.dto.response.PolicyResponse;
import com.insurancepolicyadmin.dto.response.PolicySearchResponse;
import com.insurancepolicyadmin.dto.response.PolicyStatusHistoryResponse;
import com.insurancepolicyadmin.service.PolicyService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for Policy issuance and lifecycle operations (design.md REST API table,
 * endpoints 1-6 and 11; Requirements 14.1-14.6, 14.11).
 *
 * <p>Endpoints:</p>
 * <ul>
 *   <li>{@code POST /api/policies} — issue a Policy (endpoint 1, 201 Created)</li>
 *   <li>{@code GET  /api/policies/{policyId}} — get a Policy by id (endpoint 2)</li>
 *   <li>{@code GET  /api/policies/by-number/{policyNumber}} — get a Policy by number (endpoint 3)</li>
 *   <li>{@code POST /api/policies/{policyId}/bind} — bind a quoted Policy (endpoint 4)</li>
 *   <li>{@code POST /api/policies/{policyId}/status-transitions} — transition status (endpoint 5)</li>
 *   <li>{@code GET  /api/policies/{policyId}/status-history} — status history (endpoint 6)</li>
 *   <li>{@code GET  /api/policies} — paged search (endpoint 11)</li>
 * </ul>
 *
 * <p>This is a thin controller: it performs no business logic and delegates every operation to
 * {@link PolicyService}. It is deliberately NOT one of the intentional-layering-violation
 * controllers (those are {@code PaymentController} and {@code ReportController}). Exceptions raised
 * by the service are translated to HTTP responses by the shared {@code GlobalExceptionHandler}.</p>
 */
@RestController
@RequestMapping("/policies")
public class PolicyController {

    private final PolicyService policyService;

    public PolicyController(PolicyService policyService) {
        this.policyService = policyService;
    }

    /**
     * Endpoint 1 — issues a new Policy (initial {@code quoted} status).
     *
     * @param request the Policy issuance request
     * @return 201 Created with the created {@link PolicyResponse}
     */
    @PostMapping
    public ResponseEntity<PolicyResponse> issue(@RequestBody PolicyIssueRequest request) {
        PolicyResponse response = policyService.issuePolicy(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Endpoint 2 — retrieves a Policy by identifier.
     *
     * @param policyId the Policy identifier
     * @return 200 with the {@link PolicyResponse}
     */
    @GetMapping("/{policyId}")
    public ResponseEntity<PolicyResponse> getById(@PathVariable Long policyId) {
        return ResponseEntity.ok(policyService.getById(policyId));
    }

    /**
     * Endpoint 3 — retrieves a Policy by its unique Policy number.
     *
     * @param policyNumber the Policy number
     * @return 200 with the {@link PolicyResponse}
     */
    @GetMapping("/by-number/{policyNumber}")
    public ResponseEntity<PolicyResponse> getByNumber(@PathVariable String policyNumber) {
        return ResponseEntity.ok(policyService.getByNumber(policyNumber));
    }

    /**
     * Endpoint 4 — binds a {@code quoted} Policy, recording the approving Underwriter.
     *
     * @param policyId the Policy identifier
     * @param request  the request body carrying {@code underwriterId}
     * @return 200 with the bound {@link PolicyResponse}
     */
    @PostMapping("/{policyId}/bind")
    public ResponseEntity<PolicyResponse> bind(@PathVariable Long policyId,
                                               @RequestBody BindRequest request) {
        Long underwriterId = request == null ? null : request.getUnderwriterId();
        return ResponseEntity.ok(policyService.bindPolicy(policyId, underwriterId));
    }

    /**
     * Endpoint 5 — executes a Policy status transition.
     *
     * @param policyId the Policy identifier
     * @param request  the request body carrying {@code targetStatus}
     * @return 200 with the updated {@link PolicyResponse}
     */
    @PostMapping("/{policyId}/status-transitions")
    public ResponseEntity<PolicyResponse> transitionStatus(@PathVariable Long policyId,
                                                           @RequestBody StatusTransitionRequest request) {
        String targetStatus = request == null ? null : request.getTargetStatus();
        return ResponseEntity.ok(policyService.transitionStatus(policyId, targetStatus));
    }

    /**
     * Endpoint 6 — retrieves the chronological status history for a Policy.
     *
     * @param policyId the Policy identifier
     * @return 200 with the {@link PolicyStatusHistoryResponse} records
     */
    @GetMapping("/{policyId}/status-history")
    public ResponseEntity<List<PolicyStatusHistoryResponse>> getStatusHistory(
            @PathVariable Long policyId) {
        return ResponseEntity.ok(policyService.getStatusHistory(policyId));
    }

    /**
     * Endpoint 11 — paged Policy search by any combination of criteria (AND semantics).
     *
     * @param customerId   the optional Customer-identifier criterion
     * @param policyNumber the optional Policy-number criterion
     * @param status       the optional Policy-status criterion
     * @param agentId      the optional Agent-identifier criterion
     * @param page         the zero-based page number (defaults to 0)
     * @param size         the page size (defaults to 20, capped at 100 by the service)
     * @return 200 with the paged {@link PolicySearchResponse}
     */
    @GetMapping
    public ResponseEntity<PolicySearchResponse> search(
            @RequestParam(value = "customerId", required = false) Long customerId,
            @RequestParam(value = "policyNumber", required = false) String policyNumber,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "agentId", required = false) Long agentId,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "20") int size) {
        PolicySearchCriteria criteria = new PolicySearchCriteria();
        criteria.setCustomerId(customerId);
        criteria.setPolicyNumber(policyNumber);
        criteria.setPolicyStatus(status);
        criteria.setAgentId(agentId);
        criteria.setPage(page);
        criteria.setSize(size);
        return ResponseEntity.ok(policyService.search(criteria));
    }

    /**
     * Request body for the bind endpoint (endpoint 4): {@code {"underwriterId": ...}}.
     */
    public static class BindRequest {
        private Long underwriterId;

        public Long getUnderwriterId() {
            return underwriterId;
        }

        public void setUnderwriterId(Long underwriterId) {
            this.underwriterId = underwriterId;
        }
    }

    /**
     * Request body for the status-transition endpoint (endpoint 5): {@code {"targetStatus": "..."}}.
     */
    public static class StatusTransitionRequest {
        private String targetStatus;

        public String getTargetStatus() {
            return targetStatus;
        }

        public void setTargetStatus(String targetStatus) {
            this.targetStatus = targetStatus;
        }
    }
}
