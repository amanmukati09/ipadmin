package com.insurancepolicyadmin.dto.response;

/**
 * Response DTO for an Underwriter, mirroring {@code model.plain.Underwriter}'s fields.
 */
public class UnderwriterResponse {
    private Long underwriterId;
    private String name;
    private String licenseId;
    private String activeStatus;

    public UnderwriterResponse() {
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
