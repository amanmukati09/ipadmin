package com.insurancepolicyadmin.dao;

import com.insurancepolicyadmin.model.plain.Underwriter;

import java.util.Optional;

/**
 * Technology-neutral DAO contract for {@link Underwriter} persistence.
 *
 * <p>Implemented by {@code JdbcUnderwriterDao} using {@code SimpleJdbcCall}/
 * {@code CallableStatement} through {@code JdbcTemplate} exclusively, invoking stored
 * procedures. This interface SHALL NOT reference Hibernate, JPA, {@code JdbcTemplate},
 * or any other persistence-technology-specific type in its method signatures.</p>
 */
public interface UnderwriterDao {

    /**
     * Persists a new Underwriter record.
     *
     * @param underwriter the Underwriter to create
     * @return the generated Underwriter identifier
     */
    Long create(Underwriter underwriter);

    /**
     * Retrieves an Underwriter by identifier.
     *
     * @param underwriterId the Underwriter identifier
     * @return an {@link Optional} containing the Underwriter if found, otherwise empty
     */
    Optional<Underwriter> findById(Long underwriterId);

    /**
     * Determines whether an Underwriter identifier exists and has an active status.
     *
     * @param underwriterId the Underwriter identifier
     * @return {@code true} if the Underwriter exists and is active, otherwise {@code false}
     */
    boolean isActive(Long underwriterId);
}
