package com.insurancepolicyadmin.dao.jdbc;

import java.util.Map;
import java.util.Optional;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Repository;

import com.insurancepolicyadmin.dao.UnderwriterDao;
import com.insurancepolicyadmin.model.plain.Underwriter;

/**
 * JDBC implementation of {@link UnderwriterDao}.
 *
 * <p>All persistence work is performed exclusively through {@link SimpleJdbcCall}
 * (built on {@link JdbcTemplate}), invoking the underwriter stored procedures
 * {@code sp_underwriter_create}, {@code sp_underwriter_get_by_id}, and
 * {@code sp_underwriter_is_active}. This class SHALL NOT reference Hibernate, JPA,
 * {@code EntityManager}, or any other ORM type.</p>
 */
@Repository
public class JdbcUnderwriterDao implements UnderwriterDao {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcUnderwriterDao.class);

    private SimpleJdbcCall createCall;
    private SimpleJdbcCall getByIdCall;
    private SimpleJdbcCall isActiveCall;

    /**
     * Maps a single {@code underwriter} result-set row to an {@link Underwriter}.
     */
    private static final RowMapper<Underwriter> UNDERWRITER_ROW_MAPPER = new RowMapper<Underwriter>() {
        @Override
        public Underwriter mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
            Underwriter underwriter = new Underwriter();
            long id = rs.getLong("underwriter_id");
            underwriter.setUnderwriterId(rs.wasNull() ? null : Long.valueOf(id));
            underwriter.setName(rs.getString("name"));
            underwriter.setLicenseId(rs.getString("license_id"));
            underwriter.setActiveStatus(rs.getString("active_status"));
            return underwriter;
        }
    };

    @Autowired
    public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        DataSource dataSource = jdbcTemplate.getDataSource();
        this.createCall = new SimpleJdbcCall(dataSource).withProcedureName("sp_underwriter_create");
        this.getByIdCall = new SimpleJdbcCall(dataSource)
                .withProcedureName("sp_underwriter_get_by_id")
                .returningResultSet("underwriters", UNDERWRITER_ROW_MAPPER);
        this.isActiveCall = new SimpleJdbcCall(dataSource).withProcedureName("sp_underwriter_is_active");
    }

    @Override
    public Long create(Underwriter underwriter) {
        if (underwriter == null) {
            throw new IllegalArgumentException("underwriter must not be null");
        }
        Map<String, Object> out = createCall.execute(
                underwriter.getName(),
                underwriter.getLicenseId(),
                underwriter.getActiveStatus());
        Number generatedId = (Number) out.get("p_underwriter_id");
        Long underwriterId = generatedId == null ? null : Long.valueOf(generatedId.longValue());
        underwriter.setUnderwriterId(underwriterId);
        LOGGER.debug("Created underwriter with id {}", underwriterId);
        return underwriterId;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<Underwriter> findById(Long underwriterId) {
        if (underwriterId == null) {
            return Optional.empty();
        }
        Map<String, Object> out = getByIdCall.execute(underwriterId);
        java.util.List<Underwriter> rows = (java.util.List<Underwriter>) out.get("underwriters");
        if (rows == null || rows.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(rows.get(0));
    }

    @Override
    public boolean isActive(Long underwriterId) {
        if (underwriterId == null) {
            return false;
        }
        Map<String, Object> out = isActiveCall.execute(underwriterId);
        Number isActive = (Number) out.get("p_is_active");
        return isActive != null && isActive.intValue() == 1;
    }
}
