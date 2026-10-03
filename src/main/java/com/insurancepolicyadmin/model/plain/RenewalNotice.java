package com.insurancepolicyadmin.model.plain;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Plain (non-JPA) model for the {@code renewal_notice} table.
 * Persisted exclusively through {@code JdbcReportDao} via stored procedures.
 * Contains no JPA/Hibernate annotations per Requirement 2.8.
 */
public class RenewalNotice {

    private Long renewalNoticeId;
    private Long policyId;
    private LocalDateTime generatedAt;
    private LocalDate termEndDateSnapshot;

    public RenewalNotice() {
    }

    public Long getRenewalNoticeId() {
        return renewalNoticeId;
    }

    public void setRenewalNoticeId(Long renewalNoticeId) {
        this.renewalNoticeId = renewalNoticeId;
    }

    public Long getPolicyId() {
        return policyId;
    }

    public void setPolicyId(Long policyId) {
        this.policyId = policyId;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }

    public LocalDate getTermEndDateSnapshot() {
        return termEndDateSnapshot;
    }

    public void setTermEndDateSnapshot(LocalDate termEndDateSnapshot) {
        this.termEndDateSnapshot = termEndDateSnapshot;
    }
}
