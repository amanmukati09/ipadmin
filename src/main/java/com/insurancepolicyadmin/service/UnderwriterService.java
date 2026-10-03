package com.insurancepolicyadmin.service;

import com.insurancepolicyadmin.dto.request.UnderwriterCreateRequest;
import com.insurancepolicyadmin.dto.response.UnderwriterResponse;

/**
 * Service-layer contract for Underwriter management (design.md REST API table, endpoints 27-28).
 *
 * <p>Implemented by {@code UnderwriterServiceImpl}, which delegates all Underwriter persistence to
 * the Spring-JDBC-backed {@code UnderwriterDao} (stored procedures via {@code SimpleJdbcCall}).
 * The Underwriter is a JDBC-persisted entity, so this service's transactions are bound to the JDBC
 * transaction manager.</p>
 */
public interface UnderwriterService {

    /**
     * Creates a new Underwriter record.
     *
     * @param request the Underwriter field values (name, license id, active status)
     * @return the created Underwriter as a response DTO (including the generated identifier)
     */
    UnderwriterResponse create(UnderwriterCreateRequest request);

    /**
     * Retrieves an Underwriter by identifier, raising {@code UnderwriterNotFoundException} if absent.
     *
     * @param underwriterId the Underwriter identifier (must exist)
     * @return the Underwriter as a response DTO
     */
    UnderwriterResponse getById(Long underwriterId);
}
