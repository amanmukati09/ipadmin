package com.insurancepolicyadmin.dao.hibernate;

import com.insurancepolicyadmin.dao.CustomerDao;
import com.insurancepolicyadmin.model.entity.Customer;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Hibernate-backed implementation of {@link CustomerDao}.
 *
 * <p>All Customer CRUD is performed exclusively through Hibernate session operations
 * ({@code save}, {@code get}, {@code merge}, {@code delete}) and JPA Criteria queries
 * obtained from {@code SessionFactory.getCurrentSession()}. This class contains no
 * inline SQL and invokes no stored procedures.</p>
 */
@Repository
public class HibernateCustomerDao implements CustomerDao {

    private final SessionFactory sessionFactory;

    @Autowired
    public HibernateCustomerDao(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    private Session currentSession() {
        return sessionFactory.getCurrentSession();
    }

    /** {@inheritDoc} */
    @Override
    public Long create(Customer customer) {
        Session session = currentSession();
        Long customerId = (Long) session.save(customer);
        session.flush();
        return customerId;
    }

    /** {@inheritDoc} */
    @Override
    public Optional<Customer> findById(Long customerId) {
        if (customerId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(currentSession().get(Customer.class, customerId));
    }

    /** {@inheritDoc} */
    @Override
    public List<Customer> search(String lastName, String firstName) {
        Session session = currentSession();
        CriteriaBuilder cb = session.getCriteriaBuilder();
        CriteriaQuery<Customer> query = cb.createQuery(Customer.class);
        Root<Customer> root = query.from(Customer.class);

        List<Predicate> predicates = new ArrayList<Predicate>();
        if (lastName != null) {
            predicates.add(cb.equal(root.get("lastName"), lastName));
        }
        if (firstName != null) {
            predicates.add(cb.equal(root.get("firstName"), firstName));
        }

        query.select(root);
        if (!predicates.isEmpty()) {
            query.where(predicates.toArray(new Predicate[predicates.size()]));
        }
        query.orderBy(cb.asc(root.get("lastName")), cb.asc(root.get("firstName")));

        return session.createQuery(query).setMaxResults(100).getResultList();
    }

    /** {@inheritDoc} */
    @Override
    public void update(Customer customer) {
        Session session = currentSession();
        session.merge(customer);
        session.flush();
    }

    /** {@inheritDoc} */
    @Override
    public void delete(Long customerId) {
        if (customerId == null) {
            return;
        }
        Session session = currentSession();
        Customer customer = session.get(Customer.class, customerId);
        if (customer != null) {
            session.delete(customer);
            session.flush();
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean existsByEmailOrPhone(String email, String phone) {
        if (email == null && phone == null) {
            return false;
        }

        Session session = currentSession();
        CriteriaBuilder cb = session.getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<Customer> root = query.from(Customer.class);
        query.select(cb.count(root));

        List<Predicate> predicates = new ArrayList<Predicate>();
        if (email != null) {
            predicates.add(cb.equal(root.get("email"), email));
        }
        if (phone != null) {
            predicates.add(cb.equal(root.get("phone"), phone));
        }
        query.where(cb.or(predicates.toArray(new Predicate[predicates.size()])));

        Long count = session.createQuery(query).uniqueResult();
        return count != null && count > 0L;
    }
}
