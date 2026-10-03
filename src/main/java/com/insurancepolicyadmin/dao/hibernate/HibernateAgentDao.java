package com.insurancepolicyadmin.dao.hibernate;

import java.util.Optional;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.insurancepolicyadmin.dao.AgentDao;
import com.insurancepolicyadmin.model.entity.Agent;

/**
 * Hibernate implementation of {@link AgentDao}.
 *
 * <p>All persistence work is performed exclusively through Hibernate
 * {@link SessionFactory}/{@link Session} operations. This class SHALL NOT use
 * JDBC, {@code JdbcTemplate}, stored procedures, or delegate to any class that
 * performs either.</p>
 */
@Repository
public class HibernateAgentDao implements AgentDao {

    private static final Logger LOGGER = LoggerFactory.getLogger(HibernateAgentDao.class);

    private SessionFactory sessionFactory;

    @Autowired
    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    private Session currentSession() {
        return sessionFactory.getCurrentSession();
    }

    @Override
    public Long create(Agent agent) {
        Session session = currentSession();
        session.save(agent);
        LOGGER.debug("Created agent with id {}", agent.getAgentId());
        return agent.getAgentId();
    }

    @Override
    public Optional<Agent> findById(Long agentId) {
        if (agentId == null) {
            return Optional.empty();
        }
        Agent agent = currentSession().get(Agent.class, agentId);
        return Optional.ofNullable(agent);
    }

    @Override
    public void update(Agent agent) {
        currentSession().merge(agent);
        LOGGER.debug("Updated agent with id {}", agent == null ? null : agent.getAgentId());
    }

    @Override
    public void delete(Long agentId) {
        if (agentId == null) {
            return;
        }
        Session session = currentSession();
        Agent agent = session.get(Agent.class, agentId);
        if (agent != null) {
            session.delete(agent);
            LOGGER.debug("Deleted agent with id {}", agentId);
        }
    }

    @Override
    public boolean existsByLicenseNumber(String licenseNumber) {
        if (licenseNumber == null) {
            return false;
        }
        Query<Long> query = currentSession().createQuery(
                "select count(a.agentId) from Agent a where a.licenseNumber = :licenseNumber", Long.class);
        query.setParameter("licenseNumber", licenseNumber);
        Long count = query.uniqueResult();
        return count != null && count.longValue() > 0L;
    }
}
