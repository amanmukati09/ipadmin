package com.insurancepolicyadmin.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Response DTO for a Renewal_Notice, mirroring {@code model.plain.RenewalNotice}'s fields.
 */
public class RenewalNoticeResponse {
    private Long renewalNoticeId;
    private Long policyId;
    private LocalDateTime generatedAt;
    private LocalDate termEndDateSnapshot;

    public RenewalNoticeResponse() {
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
