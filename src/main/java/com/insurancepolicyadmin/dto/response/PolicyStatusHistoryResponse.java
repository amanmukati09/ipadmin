package com.insurancepolicyadmin.dto.response;

import java.time.LocalDateTime;

/**
 * Response DTO for a Policy_Status_History record, mirroring
 * {@code model.plain.PolicyStatusHistory}'s fields.
 */
public class PolicyStatusHistoryResponse {
    private Long policyStatusHistoryId;
    private Long policyId;
    private String previousStatus;
    private String newStatus;
    private LocalDateTime transitionedAt;

    public PolicyStatusHistoryResponse() {
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
