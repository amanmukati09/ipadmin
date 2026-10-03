package com.insurancepolicyadmin.model.plain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Plain (non-JPA) model for the {@code endorsement} table.
 * Persisted exclusively through {@code JdbcEndorsementDao} via stored procedures.
 * Contains no JPA/Hibernate annotations per Requirement 2.8.
 */
public class Endorsement {

    private Long endorsementId;
    private Long policyId;
    private String description;
    private LocalDate effectiveDate;
    private BigDecimal premiumAdjustment;
    private LocalDateTime createdAt;

    public Endorsement() {
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
}
