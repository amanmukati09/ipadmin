package com.insurancepolicyadmin.model.plain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Plain (non-JPA) model for the {@code premium_schedule} table.
 * Persisted exclusively through {@code JdbcPremiumDao} via stored procedures.
 * Contains no JPA/Hibernate annotations per Requirement 2.8.
 */
public class PremiumSchedule {

    private Long premiumScheduleId;
    private Long policyId;
    private String billingFrequency;
    private BigDecimal totalPremium;
    private LocalDateTime createdAt;

    public PremiumSchedule() {
    }

    public Long getPremiumScheduleId() {
        return premiumScheduleId;
    }

    public void setPremiumScheduleId(Long premiumScheduleId) {
        this.premiumScheduleId = premiumScheduleId;
    }

    public Long getPolicyId() {
        return policyId;
    }

    public void setPolicyId(Long policyId) {
        this.policyId = policyId;
    }

    public String getBillingFrequency() {
        return billingFrequency;
    }

    public void setBillingFrequency(String billingFrequency) {
        this.billingFrequency = billingFrequency;
    }

    public BigDecimal getTotalPremium() {
        return totalPremium;
    }

    public void setTotalPremium(BigDecimal totalPremium) {
        this.totalPremium = totalPremium;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
