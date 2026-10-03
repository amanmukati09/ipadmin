package com.insurancepolicyadmin.dto.response;

import java.math.BigDecimal;

/**
 * Response DTO for a Beneficiary, mirroring {@code model.entity.Beneficiary}'s fields.
 */
public class BeneficiaryResponse {
    private Long beneficiaryId;
    private Long policyId;
    private String name;
    private String relationshipType;
    private BigDecimal allocationPercentage;

    public BeneficiaryResponse() {
    }

    public Long getBeneficiaryId() {
        return beneficiaryId;
    }

    public void setBeneficiaryId(Long beneficiaryId) {
        this.beneficiaryId = beneficiaryId;
    }

    public Long getPolicyId() {
        return policyId;
    }

    public void setPolicyId(Long policyId) {
        this.policyId = policyId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRelationshipType() {
        return relationshipType;
    }

    public void setRelationshipType(String relationshipType) {
        this.relationshipType = relationshipType;
    }

    public BigDecimal getAllocationPercentage() {
        return allocationPercentage;
    }

    public void setAllocationPercentage(BigDecimal allocationPercentage) {
        this.allocationPercentage = allocationPercentage;
    }
}
