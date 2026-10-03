package com.insurancepolicyadmin.service;

import com.insurancepolicyadmin.dto.request.AgentCreateRequest;
import com.insurancepolicyadmin.dto.request.AgentUpdateRequest;
import com.insurancepolicyadmin.dto.request.CustomerCreateRequest;
import com.insurancepolicyadmin.dto.response.AgentResponse;
import com.insurancepolicyadmin.dto.response.CustomerResponse;

import java.util.List;

/**
 * Service-layer contract for Customer management (Requirement 3) and Agent management
 * (Requirement 4).
 *
 * <p>Implemented by {@code CustomerServiceImpl}, which delegates all Customer persistence to
 * the Hibernate-backed {@code CustomerDao} and all Agent persistence to the Hibernate-backed
 * {@code AgentDao}. Field validation uses the static {@code util.ValidationUtils} helpers
 * (email format, phone format, non-future date of birth), aggregating per-field failures into
 * a single {@code ValidationException}. Duplicate Customer email or phone submissions are
 * rejected via {@code CustomerDao.existsByEmailOrPhone}; duplicate Agent license numbers are
 * rejected via {@code AgentDao.existsByLicenseNumber}.</p>
 */
public interface CustomerService {
    /**
     * Creates a new Customer record after validating its fields and rejecting duplicates.
     *
     * @param request the Customer field values (first name, last name, date of birth, and
     *                contact information consisting of an email and/or phone)
     * @return the created Customer as a response DTO (including the generated identifier)
     */
    CustomerResponse create(CustomerCreateRequest request);

    /**
     * Searches Customer records by last name and/or first name, requiring at least one
     * criterion. Results are ordered by last name ascending then first name ascending and
     * limited to 100 records; a non-matching search yields an empty list rather than an error.
     *
     * @param lastName  the last name criterion, or {@code null}/blank to omit it
     * @param firstName the first name criterion, or {@code null}/blank to omit it
     * @return the matching Customers (up to 100), or an empty list if none match
     */
    List<CustomerResponse> search(String lastName, String firstName);

    /**
     * Retrieves a Customer by identifier.
     *
     * @param customerId the Customer identifier (must exist)
     * @return the Customer as a response DTO
     */
    CustomerResponse getById(Long customerId);

    /**
     * Creates a new Agent record after validating its fields and rejecting a duplicate license
     * number (Req 4.1, 4.2). Delegates persistence to {@code AgentDao}.
     *
     * @param request the Agent field values (name, license number, and contact information)
     * @return the created Agent as a response DTO (including the generated identifier)
     */
    AgentResponse createAgent(AgentCreateRequest request);

    /**
     * Retrieves an Agent by identifier (Req 4.3), returning a not-found error if absent
     * (Req 4.4).
     *
     * @param agentId the Agent identifier (must exist)
     * @return the Agent as a response DTO
     */
    AgentResponse getAgentById(Long agentId);

    /**
     * Updates an existing Agent record after validating its fields (Req 4.5); returns a
     * not-found error if the identifier is absent (Req 4.7). If the license number changes,
     * a duplicate check is re-applied (Req 4.2).
     *
     * @param agentId the Agent identifier (must exist)
     * @param request the updated Agent field values
     * @return the updated Agent as a response DTO
     */
    AgentResponse updateAgent(Long agentId, AgentUpdateRequest request);

    /**
     * Deletes an Agent record by identifier (Req 4.6); returns a not-found error if the
     * identifier is absent (Req 4.7).
     *
     * @param agentId the Agent identifier (must exist)
     */
    void deleteAgent(Long agentId);
}
