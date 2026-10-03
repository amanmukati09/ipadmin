package com.insurancepolicyadmin.dto.request;

/**
 * Request DTO for {@code POST /api/underwriters}.
 */
public class UnderwriterCreateRequest {

    private String name;
    private String licenseId;
    private String activeStatus;

    public UnderwriterCreateRequest() {
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
