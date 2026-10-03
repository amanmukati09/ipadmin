package com.insurancepolicyadmin.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Request DTO for {@code POST /api/policies/{policyId}/endorsements}.
 *
 * <p>{@code description} must be 1 to 1000 characters and {@code effectiveDate}
 * must fall within the Policy's current term per Requirements 9.1, 9.3, 9.4.</p>
 */
public class EndorsementRequest {

    private String description;
    private LocalDate effectiveDate;
    private List<CoverageChange> coverageChanges;

    public EndorsementRequest() {
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

    public List<CoverageChange> getCoverageChanges() {
        return coverageChanges;
    }

    public void setCoverageChanges(List<CoverageChange> coverageChanges) {
        this.coverageChanges = coverageChanges;
    }

    /**
     * A single Coverage change (add/remove/modify limit) applied by an Endorsement.
     */
    public static class CoverageChange {

        private Long coverageTypeId;
        private BigDecimal newCoverageLimit;
        private boolean remove;

        public CoverageChange() {
        }

        public Long getCoverageTypeId() {
            return coverageTypeId;
        }

        public void setCoverageTypeId(Long coverageTypeId) {
            this.coverageTypeId = coverageTypeId;
        }

        public BigDecimal getNewCoverageLimit() {
            return newCoverageLimit;
        }

        public void setNewCoverageLimit(BigDecimal newCoverageLimit) {
            this.newCoverageLimit = newCoverageLimit;
        }

        public boolean isRemove() {
            return remove;
        }

        public void setRemove(boolean remove) {
            this.remove = remove;
        }
    }
}
