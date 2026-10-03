package com.insurancepolicyadmin.model.plain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Plain (non-JPA) model for the {@code premium_adjustment_history} table.
 * Persisted exclusively through {@code JdbcEndorsementDao} via stored procedures.
 * Contains no JPA/Hibernate annotations per Requirement 2.8.
 */
public class PremiumAdjustmentHistory {

    private Long premiumAdjustmentHistoryId;
    private Long policyId;
    private Long endorsementId;
    private BigDecimal previousPremium;
    private BigDecimal newPremium;
    private BigDecimal adjustmentAmount;
    private LocalDateTime adjustedAt;

    public PremiumAdjustmentHistory() {
    }

    public Long getPremiumAdjustmentHistoryId() {
        return premiumAdjustmentHistoryId;
    }

    public void setPremiumAdjustmentHistoryId(Long premiumAdjustmentHistoryId) {
        this.premiumAdjustmentHistoryId = premiumAdjustmentHistoryId;
    }

    public Long getPolicyId() {
        return policyId;
    }

    public void setPolicyId(Long policyId) {
        this.policyId = policyId;
    }

    public Long getEndorsementId() {
        return endorsementId;
    }

    public void setEndorsementId(Long endorsementId) {
        this.endorsementId = endorsementId;
    }

    public BigDecimal getPreviousPremium() {
        return previousPremium;
    }

    public void setPreviousPremium(BigDecimal previousPremium) {
        this.previousPremium = previousPremium;
    }

    public BigDecimal getNewPremium() {
        return newPremium;
    }

    public void setNewPremium(BigDecimal newPremium) {
        this.newPremium = newPremium;
    }

    public BigDecimal getAdjustmentAmount() {
        return adjustmentAmount;
    }

    public void setAdjustmentAmount(BigDecimal adjustmentAmount) {
        this.adjustmentAmount = adjustmentAmount;
    }

    public LocalDateTime getAdjustedAt() {
        return adjustedAt;
    }

    public void setAdjustedAt(LocalDateTime adjustedAt) {
        this.adjustedAt = adjustedAt;
    }
}
