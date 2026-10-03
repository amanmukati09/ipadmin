package com.insurancepolicyadmin.dao;

import com.insurancepolicyadmin.model.entity.Agent;

import java.util.Optional;

/**
 * Technology-neutral DAO contract for {@link Agent} persistence.
 *
 * <p>Implemented by {@code HibernateAgentDao} using Hibernate session operations
 * exclusively. This interface SHALL NOT reference Hibernate, JPA, {@code JdbcTemplate},
 * or any other persistence-technology-specific type in its method signatures.</p>
 */
public interface AgentDao {

    /**
     * Persists a new Agent record.
     *
     * @param agent the Agent to create
     * @return the generated Agent identifier
     */
    Long create(Agent agent);

    /**
     * Retrieves an Agent by identifier.
     *
     * @param agentId the Agent identifier
     * @return an {@link Optional} containing the Agent if found, otherwise empty
     */
    Optional<Agent> findById(Long agentId);

    /**
     * Persists changes to an existing Agent record.
     *
     * @param agent the Agent with updated field values
     */
    void update(Agent agent);

    /**
     * Removes an Agent record by identifier.
     *
     * @param agentId the Agent identifier
     */
    void delete(Long agentId);

    /**
     * Determines whether an Agent record already exists with the given license number.
     *
     * @param licenseNumber the license number to check
     * @return {@code true} if a matching Agent record exists, otherwise {@code false}
     */
    boolean existsByLicenseNumber(String licenseNumber);
}
