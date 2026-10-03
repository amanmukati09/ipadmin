package com.insurancepolicyadmin.dto.request;

/**
 * Request DTO for {@code POST /api/agents}.
 */
public class AgentCreateRequest {

    private String name;
    private String licenseNumber;
    private String email;
    private String phone;

    public AgentCreateRequest() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public void setLicenseNumber(String licenseNumber) {
        this.licenseNumber = licenseNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
