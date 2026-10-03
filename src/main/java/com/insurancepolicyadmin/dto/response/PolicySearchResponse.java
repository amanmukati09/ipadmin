package com.insurancepolicyadmin.dto.response;

import java.util.List;

/**
 * Response DTO for {@code GET /api/policies} (design.md REST API table, endpoint 11),
 * a paged Policy search result. Wraps the matching {@link PolicyResponse} records
 * (limited to a maximum of 100 per Requirement 3.2/18.12-style page bounds, default page
 * size 20 per Requirement 14.12) together with pagination metadata.
 */
public class PolicySearchResponse {
    private List<PolicyResponse> policies;
    private int totalCount;
    private int page;
    private int size;

    public PolicySearchResponse() {
    }

    public List<PolicyResponse> getPolicies() {
        return policies;
    }

    public void setPolicies(List<PolicyResponse> policies) {
        this.policies = policies;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }
}
