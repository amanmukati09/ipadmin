package com.insurancepolicyadmin.model.plain;

import java.time.LocalDateTime;

/**
 * Plain (non-JPA) model for the {@code policy_status_history} table.
 * Persisted exclusively through {@code JdbcPolicyDao} via stored procedures.
 * Contains no JPA/Hibernate annotations per Requirement 2.8.
 */
public class PolicyStatusHistory {

    private Long policyStatusHistoryId;
    private Long policyId;
    private String previousStatus;
    private String newStatus;
    private LocalDateTime transitionedAt;

    public PolicyStatusHistory() {
    }

    public Long getPolicyStatusHistoryId() {
        return policyStatusHistoryId;
    }

    public void setPolicyStatusHistoryId(Long policyStatusHistoryId) {
        this.policyStatusHistoryId = policyStatusHistoryId;
    }

    public Long getPolicyId() {
        return policyId;
    }

    public void setPolicyId(Long policyId) {
        this.policyId = policyId;
    }

    public String getPreviousStatus() {
        return previousStatus;
    }

    public void setPreviousStatus(String previousStatus) {
        this.previousStatus = previousStatus;
    }

    public String getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(String newStatus) {
        this.newStatus = newStatus;
    }

    public LocalDateTime getTransitionedAt() {
        return transitionedAt;
    }

    public void setTransitionedAt(LocalDateTime transitionedAt) {
        this.transitionedAt = transitionedAt;
    }
}
