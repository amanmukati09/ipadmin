package com.insurancepolicyadmin.service;

import com.insurancepolicyadmin.dto.response.OverdueInstallmentResponse;
import com.insurancepolicyadmin.dto.response.PolicySummaryResponse;
import com.insurancepolicyadmin.dto.response.RenewalNoticeResponse;

import java.util.List;

/**
 * Service contract for reporting-style operations and Renewal_Notice generation.
 *
 * <p>Business rules (lookahead/day-count range validation, Agent existence checks,
 * empty-result handling) live in {@code ReportServiceImpl}; the actual reporting queries
 * and Renewal_Notice persistence are delegated to {@code ReportDao} (its {@code JdbcReportDao}
 * implementation invokes MySQL stored procedures exclusively, Requirements 12.3, 13.9).</p>
 */
public interface ReportService {

    /**
     * Generates Renewal_Notices for all active Policies whose current term end date falls
     * within the supplied lookahead window from the current date.
     *
     * <p>The lookahead window MUST be between 1 and 365 days inclusive; a window of zero, a
     * negative value, or a value greater than 365 is rejected with a validation error
     * (Requirement 12.2). For each matching Policy, a Renewal_Notice is generated if one does
     * not already exist for the Policy's current renewal cycle; otherwise the pre-existing
     * notice is returned (duplicate-cycle detection, Requirement 12.4).</p>
     *
     * @param lookaheadDays the lookahead window in days (1 to 365 inclusive)
     * @return the generated or pre-existing Renewal_Notices, one per matching Policy; an empty
     *         list if no active Policy falls within the window
     * @throws com.insurancepolicyadmin.exception.ValidationException if {@code lookaheadDays} is
     *         outside the range 1 to 365 inclusive
     */
    List<RenewalNoticeResponse> generateRenewalNotices(int lookaheadDays);

    /**
     * Retrieves active Policies whose current term end date falls within the given number of
     * days from the current date.
     *
     * @param dayCount the lookahead window in days
     * @return the matching Policy summaries; an empty list if {@code dayCount} is zero or
     *         negative, or if no Policy matches (Requirements 13.2, 13.3)
     */
    List<PolicySummaryResponse> getExpiringPolicies(int dayCount);

    /**
     * Retrieves Installments that are unpaid and past their due date, across all Policies.
     *
     * @return the matching overdue Installments; an empty list if none match (Requirement 13.5)
     */
    List<OverdueInstallmentResponse> getOverduePayments();

    /**
     * Retrieves the Policies attributed to an Agent, including each Policy's current status and
     * current premium amount.
     *
     * @param agentId the Agent identifier
     * @return the Agent's Policy summaries; an empty list if the Agent exists but has zero
     *         attributed Policies (Requirement 13.7)
     * @throws com.insurancepolicyadmin.exception.AgentNotFoundException if no Agent exists with
     *         the given identifier (Requirement 13.8)
     */
    List<PolicySummaryResponse> getAgentBookOfBusiness(Long agentId);

    /**
     * Exports an Agent's book-of-business report, returning the rendered export payload.
     *
     * @param agentId the Agent identifier
     * @return the rendered export payload
     * @throws com.insurancepolicyadmin.exception.AgentNotFoundException if no Agent exists with
     *         the given identifier
     * @throws com.insurancepolicyadmin.exception.ReportExportException if the export fails
     */
    String exportBookOfBusinessReport(Long agentId);
}
