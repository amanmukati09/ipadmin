package com.insurancepolicyadmin.model.plain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Plain (non-JPA) model for the {@code coverage} table.
 * Persisted exclusively through {@code JdbcPolicyDao}/{@code JdbcEndorsementDao} via stored procedures.
 * Contains no JPA/Hibernate annotations per Requirement 2.8.
 */
public class Coverage {

    private Long coverageId;
    private Long policyId;
    private Long coverageTypeId;
    private BigDecimal coverageLimit;
    private LocalDateTime createdAt;

    public Coverage() {
    }

    public Long getCoverageId() {
        return coverageId;
    }

    public void setCoverageId(Long coverageId) {
        this.coverageId = coverageId;
    }

    public Long getPolicyId() {
        return policyId;
    }

    public void setPolicyId(Long policyId) {
        this.policyId = policyId;
    }

    public Long getCoverageTypeId() {
        return coverageTypeId;
    }

    public void setCoverageTypeId(Long coverageTypeId) {
        this.coverageTypeId = coverageTypeId;
    }

    public BigDecimal getCoverageLimit() {
        return coverageLimit;
    }

    public void setCoverageLimit(BigDecimal coverageLimit) {
        this.coverageLimit = coverageLimit;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
