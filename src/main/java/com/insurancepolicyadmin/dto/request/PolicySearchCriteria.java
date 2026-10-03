package com.insurancepolicyadmin.dto.request;

/**
 * Request DTO for {@code GET /api/policies} search/filter query parameters.
 *
 * <p><strong>INTENTIONAL mutable JavaBean smell (Requirement 16.4):</strong> this class
 * is a bare mutable bean by design, used as a fixture to exercise detection of this
 * anti-pattern. It deliberately has no validation, no final fields, no defensive
 * copying, and no builder pattern — just plain public getters/setters.</p>
 */
public class PolicySearchCriteria {

    private Long customerId;
    private String policyNumber;
    private String policyStatus;
    private Long agentId;
    private int page;
    private int size;

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getPolicyNumber() {
        return policyNumber;
    }

    public void setPolicyNumber(String policyNumber) {
        this.policyNumber = policyNumber;
    }

    public String getPolicyStatus() {
        return policyStatus;
    }

    public void setPolicyStatus(String policyStatus) {
        this.policyStatus = policyStatus;
    }

    public Long getAgentId() {
        return agentId;
    }

    public void setAgentId(Long agentId) {
        this.agentId = agentId;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }
}
