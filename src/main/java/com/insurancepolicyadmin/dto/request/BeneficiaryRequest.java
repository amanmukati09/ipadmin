package com.insurancepolicyadmin.dto.request;

import java.math.BigDecimal;

/**
 * Request DTO for {@code POST /api/policies/{policyId}/beneficiaries} and
 * {@code PUT /api/beneficiaries/{beneficiaryId}}.
 */
public class BeneficiaryRequest {

    private String name;
    private String relationshipType;
    private BigDecimal allocationPercentage;

    public BeneficiaryRequest() {
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
