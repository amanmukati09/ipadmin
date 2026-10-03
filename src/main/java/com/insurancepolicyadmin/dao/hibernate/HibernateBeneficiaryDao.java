package com.insurancepolicyadmin.dao.hibernate;

import com.insurancepolicyadmin.dao.BeneficiaryDao;
import com.insurancepolicyadmin.model.entity.Beneficiary;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Hibernate-backed implementation of {@link BeneficiaryDao}.
 *
 * <p>All Beneficiary CRUD is performed exclusively through Hibernate session
 * operations ({@code persist}, {@code get}, {@code merge}, {@code delete}) and HQL
 * queries obtained from {@code SessionFactory.getCurrentSession()}. This class contains
 * no inline SQL and invokes no stored procedures.</p>
 *
 * <p>{@code policyId} is compared as a scalar foreign-key field rather than through an
 * association join, because {@code Policy} is not a Hibernate-mapped JPA entity.</p>
 */
@Repository
public class HibernateBeneficiaryDao implements BeneficiaryDao {

    private final SessionFactory sessionFactory;

    @Autowired
    public HibernateBeneficiaryDao(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    private Session currentSession() {
        return sessionFactory.getCurrentSession();
    }

    /** {@inheritDoc} */
    @Override
    public Long create(Beneficiary beneficiary) {
        Session session = currentSession();
        session.persist(beneficiary);
        session.flush();
        return beneficiary.getBeneficiaryId();
    }

    /** {@inheritDoc} */
    @Override
    public Optional<Beneficiary> findById(Long beneficiaryId) {
        if (beneficiaryId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(currentSession().get(Beneficiary.class, beneficiaryId));
    }

    /** {@inheritDoc} */
    @Override
    public List<Beneficiary> findAllByPolicyId(Long policyId) {
        Query<Beneficiary> query = currentSession().createQuery(
                "from Beneficiary b where b.policyId = :policyId order by b.beneficiaryId",
                Beneficiary.class);
        query.setParameter("policyId", policyId);
        return query.getResultList();
    }

    /** {@inheritDoc} */
    @Override
    public void update(Beneficiary beneficiary) {
        Session session = currentSession();
        session.merge(beneficiary);
        session.flush();
    }

    /** {@inheritDoc} */
    @Override
    public void delete(Long beneficiaryId) {
        Session session = currentSession();
        Beneficiary beneficiary = session.get(Beneficiary.class, beneficiaryId);
        if (beneficiary != null) {
            session.delete(beneficiary);
            session.flush();
        }
    }

    /** {@inheritDoc} */
    @Override
    public BigDecimal sumAllocationByPolicyId(Long policyId) {
        Query<BigDecimal> query = currentSession().createQuery(
                "select sum(b.allocationPercentage) from Beneficiary b where b.policyId = :policyId",
                BigDecimal.class);
        query.setParameter("policyId", policyId);
        BigDecimal sum = query.uniqueResult();
        return sum == null ? BigDecimal.ZERO : sum;
    }
}
