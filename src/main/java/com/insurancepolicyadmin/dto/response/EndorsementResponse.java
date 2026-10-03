package com.insurancepolicyadmin.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Response DTO for an Endorsement, mirroring {@code model.plain.Endorsement}'s fields,
 * plus the updated {@link PolicyResponse} per design.md's REST API table (endpoint 7:
 * {@code POST /api/policies/{policyId}/endorsements} returns the Endorsement details
 * together with the Policy's updated state after the endorsement is applied).
 */
public class EndorsementResponse {
    private Long endorsementId;
    private Long policyId;
    private String description;
    private LocalDate effectiveDate;
    private BigDecimal premiumAdjustment;
    private LocalDateTime createdAt;
    private PolicyResponse policy;

    public EndorsementResponse() {
    }

    public Long getEndorsementId() {
        return endorsementId;
    }

    public void setEndorsementId(Long endorsementId) {
        this.endorsementId = endorsementId;
    }

    public Long getPolicyId() {
        return policyId;
    }

    public void setPolicyId(Long policyId) {
        this.policyId = policyId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(LocalDate effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    public BigDecimal getPremiumAdjustment() {
        return premiumAdjustment;
    }

    public void setPremiumAdjustment(BigDecimal premiumAdjustment) {
        this.premiumAdjustment = premiumAdjustment;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public PolicyResponse getPolicy() {
        return policy;
    }

    public void setPolicy(PolicyResponse policy) {
        this.policy = policy;
    }
}
