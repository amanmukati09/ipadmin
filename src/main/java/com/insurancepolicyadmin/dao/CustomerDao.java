package com.insurancepolicyadmin.dao;

import com.insurancepolicyadmin.model.entity.Customer;

import java.util.List;
import java.util.Optional;

/**
 * Technology-neutral DAO contract for {@link Customer} persistence.
 *
 * <p>Implemented by {@code HibernateCustomerDao} using Hibernate session operations
 * exclusively. This interface SHALL NOT reference Hibernate, JPA, {@code JdbcTemplate},
 * or any other persistence-technology-specific type in its method signatures.</p>
 */
public interface CustomerDao {

    /**
     * Persists a new Customer record.
     *
     * @param customer the Customer to create
     * @return the generated Customer identifier
     */
    Long create(Customer customer);

    /**
     * Retrieves a Customer by identifier.
     *
     * @param customerId the Customer identifier
     * @return an {@link Optional} containing the Customer if found, otherwise empty
     */
    Optional<Customer> findById(Long customerId);

    /**
     * Searches Customer records by last name and/or first name, ordered by last name
     * ascending and, for identical last names, first name ascending, limited to 100 results.
     *
     * @param lastName  the last name criterion, or {@code null} to omit this criterion
     * @param firstName the first name criterion, or {@code null} to omit this criterion
     * @return the matching Customer records, up to 100, ordered by last name then first name
     */
    List<Customer> search(String lastName, String firstName);

    /**
     * Persists changes to an existing Customer record.
     *
     * @param customer the Customer with updated field values
     */
    void update(Customer customer);

    /**
     * Removes a Customer record by identifier.
     *
     * @param customerId the Customer identifier
     */
    void delete(Long customerId);

    /**
     * Determines whether a Customer record already exists with the given email address
     * or phone number.
     *
     * @param email the email address to check, or {@code null} to omit this criterion
     * @param phone the phone number to check, or {@code null} to omit this criterion
     * @return {@code true} if a matching Customer record exists, otherwise {@code false}
     */
    boolean existsByEmailOrPhone(String email, String phone);
}
