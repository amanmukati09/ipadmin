package com.insurancepolicyadmin.model.plain;

/**
 * Plain (non-JPA) model for the {@code underwriter} table.
 * Persisted exclusively through {@code JdbcUnderwriterDao} via stored procedures.
 * Contains no JPA/Hibernate annotations per Requirement 2.8.
 */
public class Underwriter {

    private Long underwriterId;
    private String name;
    private String licenseId;
    private String activeStatus;

    public Underwriter() {
    }

    public Long getUnderwriterId() {
        return underwriterId;
    }

    public void setUnderwriterId(Long underwriterId) {
        this.underwriterId = underwriterId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLicenseId() {
        return licenseId;
    }

    public void setLicenseId(String licenseId) {
        this.licenseId = licenseId;
    }

    public String getActiveStatus() {
        return activeStatus;
    }

    public void setActiveStatus(String activeStatus) {
        this.activeStatus = activeStatus;
    }
}
