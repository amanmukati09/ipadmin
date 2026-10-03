package com.insurancepolicyadmin.model.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.math.BigDecimal;

/**
 * Hibernate-mapped JPA entity for the {@code beneficiary} table.
 * Persisted exclusively through {@code HibernateBeneficiaryDao}.
 *
 * <p>{@code policyId} is a scalar foreign-key column, not a {@code @ManyToOne}
 * relationship, because {@code Policy} is not a Hibernate-mapped JPA entity
 * (Policy persistence is handled exclusively via stored procedures/JDBC).</p>
 */
@Entity
@Table(name = "beneficiary")
public class Beneficiary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "beneficiary_id")
    private Long beneficiaryId;

    @Column(name = "policy_id", nullable = false)
    private Long policyId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "relationship_type", nullable = false, length = 20)
    private String relationshipType;

    @Column(name = "allocation_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal allocationPercentage;

    public Beneficiary() {
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
