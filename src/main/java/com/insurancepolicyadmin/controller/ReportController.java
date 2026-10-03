package com.insurancepolicyadmin.controller;

import com.insurancepolicyadmin.dto.request.RenewalNoticeRequest;
import com.insurancepolicyadmin.dto.response.OverdueInstallmentResponse;
import com.insurancepolicyadmin.dto.response.PolicySummaryResponse;
import com.insurancepolicyadmin.dto.response.RenewalNoticeResponse;
import com.insurancepolicyadmin.exception.ValidationException;
import com.insurancepolicyadmin.service.ReportService;
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
 * REST endpoints for Renewal_Notice generation and reporting-style queries
 * (REST table endpoints 12-15).
 *
 * <p>The base paths intentionally diverge across the endpoints: Renewal_Notice generation is
 * exposed at {@code /api/renewal-notices}, the expiring-policies and overdue-payments reports at
 * {@code /api/reports/*}, and an Agent's book of business at {@code /api/agents/{agentId}/...};
 * the class-level mapping is therefore the common {@code /api} prefix with full sub-paths per
 * method.</p>
 *
 * <p>Validates: Requirements 12.x, 13.x, 16.3.</p>
 */
@RestController
@RequestMapping
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    /**
     * Endpoint 12 — generates Renewal_Notices for active Policies whose term end date falls
     * within the supplied lookahead window.
     *
     * @param request the request body carrying {@code lookaheadDays}
     * @return 200 with the generated or pre-existing Renewal_Notices
     */
    @PostMapping("/renewal-notices")
    public ResponseEntity<List<RenewalNoticeResponse>> generateRenewalNotice(
            @RequestBody RenewalNoticeRequest request) {
        int lookaheadDays = request.getLookaheadDays();
        // INTENTIONAL LAYERING VIOLATION
        if (lookaheadDays < 1 || lookaheadDays > 365) {
            throw new ValidationException(
                    "lookaheadDays must be between 1 and 365 inclusive");
        }
        List<RenewalNoticeResponse> notices = reportService.generateRenewalNotices(lookaheadDays);
        return ResponseEntity.ok(notices);
    }

    /**
     * Endpoint 13 — retrieves active Policies whose term end date falls within the given number
     * of days from the current date.
     *
     * @param days the lookahead window in days
     * @return 200 with the matching Policy summaries
     */
    @GetMapping("/reports/expiring-policies")
    public ResponseEntity<List<PolicySummaryResponse>> getExpiringPolicies(
            @RequestParam("days") int days) {
        List<PolicySummaryResponse> policies = reportService.getExpiringPolicies(days);
        return ResponseEntity.ok(policies);
    }

    /**
     * Endpoint 14 — retrieves Installments that are unpaid and past their due date across all
     * Policies.
     *
     * @return 200 with the matching overdue Installments
     */
    @GetMapping("/reports/overdue-payments")
    public ResponseEntity<List<OverdueInstallmentResponse>> getOverduePayments() {
        List<OverdueInstallmentResponse> overdue = reportService.getOverduePayments();
        return ResponseEntity.ok(overdue);
    }

    /**
     * Endpoint 15 — retrieves the Policies attributed to an Agent, including each Policy's
     * current status and current premium amount.
     *
     * @param agentId the Agent identifier
     * @return 200 with the Agent's Policy summaries
     */
    @GetMapping("/agents/{agentId}/book-of-business")
    public ResponseEntity<List<PolicySummaryResponse>> getAgentBookOfBusiness(
            @PathVariable("agentId") Long agentId) {
        List<PolicySummaryResponse> book = reportService.getAgentBookOfBusiness(agentId);
        return ResponseEntity.ok(book);
    }

    /**
     * Endpoint 15 (export) — exports an Agent's book-of-business report, returning the rendered
     * export payload.
     *
     * @param agentId the Agent identifier
     * @return 200 with the rendered export payload
     */
    @GetMapping("/agents/{agentId}/book-of-business/export")
    public ResponseEntity<String> exportBookOfBusinessReport(
            @PathVariable("agentId") Long agentId) {
        String payload = reportService.exportBookOfBusinessReport(agentId);
        return ResponseEntity.ok(payload);
    }
}
