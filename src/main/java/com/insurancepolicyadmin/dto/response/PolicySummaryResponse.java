package com.insurancepolicyadmin.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Lightweight list-view projection of a Policy, used by
 * {@code GET /api/agents/{agentId}/book-of-business} (design.md REST API table,
 * endpoint 15). Carries the subset of {@code model.plain.Policy}'s fields relevant to a
 * book-of-business summary, omitting audit metadata (e.g. {@code createdAt}) that a
 * full {@link PolicyResponse} would include.
 */
public class PolicySummaryResponse {
    private Long policyId;
    private String policyNumber;
    private Long customerId;
    private String policyStatus;
    private LocalDate termEndDate;
    private BigDecimal totalPremium;

    public PolicySummaryResponse() {
    }

    public Long getPolicyId() {
        return policyId;
    }

    public void setPolicyId(Long policyId) {
        this.policyId = policyId;
    }

    public String getPolicyNumber() {
        return policyNumber;
    }

    public void setPolicyNumber(String policyNumber) {
        this.policyNumber = policyNumber;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getPolicyStatus() {
        return policyStatus;
    }

    public void setPolicyStatus(String policyStatus) {
        this.policyStatus = policyStatus;
    }

    public LocalDate getTermEndDate() {
        return termEndDate;
    }

    public void setTermEndDate(LocalDate termEndDate) {
        this.termEndDate = termEndDate;
    }

    public BigDecimal getTotalPremium() {
        return totalPremium;
    }

    public void setTotalPremium(BigDecimal totalPremium) {
        this.totalPremium = totalPremium;
    }
}
