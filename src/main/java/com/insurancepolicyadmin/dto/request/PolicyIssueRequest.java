package com.insurancepolicyadmin.dto.request;

import java.time.LocalDate;
import java.util.List;

/**
 * Request DTO for {@code POST /api/policies} (Policy issuance).
 *
 * <p>{@code coverages} must contain between 1 and 50 entries per Requirement 7.4.</p>
 */
public class PolicyIssueRequest {

    private Long customerId;
    private Long agentId;
    private LocalDate termStartDate;
    private LocalDate termEndDate;
    private String billingFrequency;
    private java.math.BigDecimal totalPremium;
    private List<CoverageItem> coverages;

    public PolicyIssueRequest() {
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public Long getAgentId() {
        return agentId;
    }

    public void setAgentId(Long agentId) {
        this.agentId = agentId;
    }

    public LocalDate getTermStartDate() {
        return termStartDate;
    }

    public void setTermStartDate(LocalDate termStartDate) {
        this.termStartDate = termStartDate;
    }

    public LocalDate getTermEndDate() {
        return termEndDate;
    }

    public void setTermEndDate(LocalDate termEndDate) {
        this.termEndDate = termEndDate;
    }

    public String getBillingFrequency() {
        return billingFrequency;
    }

    public void setBillingFrequency(String billingFrequency) {
        this.billingFrequency = billingFrequency;
    }

    public java.math.BigDecimal getTotalPremium() {
        return totalPremium;
    }

    public void setTotalPremium(java.math.BigDecimal totalPremium) {
        this.totalPremium = totalPremium;
    }

    public List<CoverageItem> getCoverages() {
        return coverages;
    }

    public void setCoverages(List<CoverageItem> coverages) {
        this.coverages = coverages;
    }

    /**
     * A single Coverage definition (coverage type + limit) supplied as part of
     * a Policy issuance request.
     */
    public static class CoverageItem {

        private Long coverageTypeId;
        private java.math.BigDecimal coverageLimit;

        public CoverageItem() {
        }

        public Long getCoverageTypeId() {
            return coverageTypeId;
        }

        public void setCoverageTypeId(Long coverageTypeId) {
            this.coverageTypeId = coverageTypeId;
        }

        public java.math.BigDecimal getCoverageLimit() {
            return coverageLimit;
        }

        public void setCoverageLimit(java.math.BigDecimal coverageLimit) {
            this.coverageLimit = coverageLimit;
        }
    }
}
