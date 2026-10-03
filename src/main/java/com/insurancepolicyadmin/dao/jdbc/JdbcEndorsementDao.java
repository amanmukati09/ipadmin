package com.insurancepolicyadmin.dao.jdbc;

import com.insurancepolicyadmin.dao.EndorsementDao;
import com.insurancepolicyadmin.exception.PolicyNotEndorsableException;
import com.insurancepolicyadmin.model.plain.Endorsement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Spring JDBC implementation of {@link EndorsementDao}.
 *
 * <p>Every persistence operation is performed by invoking a MySQL stored procedure through
 * {@link SimpleJdbcCall} over a {@link JdbcTemplate}, with the single documented exception of
 * {@link #applyProrationAndRecordHistory(Long, Long, BigDecimal)}: no stored procedure exists
 * for the {@code premium_adjustment_history} table, so that method reads the current premium,
 * updates {@code policy.total_premium}, and inserts the history row via direct
 * {@link JdbcTemplate} statements inside a single transaction. This class references no
 * Hibernate {@code Session}, no JPA {@code EntityManager}, and no {@code @Entity} type
 * whatsoever, satisfying the strict JDBC/stored-procedure boundary (Requirements 2.5, 9.1,
 * 9.7, 9.11).</p>
 */
@Repository
public class JdbcEndorsementDao implements EndorsementDao {

    /** OUT status code returned by {@code sp_endorsement_calculate_proration} when the Policy is not endorsable. */
    private static final int STATUS_NOT_ENDORSABLE = 1;

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public JdbcEndorsementDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Long create(Long policyId, String description, LocalDate effectiveDate, BigDecimal premiumAdjustment) {
        SimpleJdbcCall call = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("sp_endorsement_create")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(
                        new SqlParameter("p_policy_id", Types.BIGINT),
                        new SqlParameter("p_description", Types.VARCHAR),
                        new SqlParameter("p_effective_date", Types.DATE),
                        new SqlParameter("p_premium_adjustment", Types.DECIMAL),
                        new SqlOutParameter("p_endorsement_id", Types.BIGINT));
        SqlParameterSource in = new MapSqlParameterSource()
                .addValue("p_policy_id", policyId)
                .addValue("p_description", description, Types.VARCHAR)
                .addValue("p_effective_date", toSqlDate(effectiveDate))
                .addValue("p_premium_adjustment", premiumAdjustment, Types.DECIMAL);
        Map<String, Object> out = call.execute(in);
        return ((Number) out.get("p_endorsement_id")).longValue();
    }

    @Override
    public Optional<Endorsement> findById(Long endorsementId) {
        SimpleJdbcCall call = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("sp_endorsement_get_by_id")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(new SqlParameter("p_endorsement_id", Types.BIGINT))
                .returningResultSet("endorsementRow", new EndorsementRowMapper());
        Map<String, Object> out = call.execute(new MapSqlParameterSource()
                .addValue("p_endorsement_id", endorsementId));
        return firstOrEmpty(extractEndorsements(out.get("endorsementRow")));
    }

    @Override
    public List<Endorsement> findAllByPolicyId(Long policyId) {
        SimpleJdbcCall call = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("sp_endorsement_list_by_policy_id")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(new SqlParameter("p_policy_id", Types.BIGINT))
                .returningResultSet("endorsementRows", new EndorsementRowMapper());
        Map<String, Object> out = call.execute(new MapSqlParameterSource()
                .addValue("p_policy_id", policyId));
        return extractEndorsements(out.get("endorsementRows"));
    }

    @Override
    public BigDecimal calculateProration(Long policyId, BigDecimal coverageDelta, LocalDate effectiveDate) {
        SimpleJdbcCall call = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("sp_endorsement_calculate_proration")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(
                        new SqlParameter("p_policy_id", Types.BIGINT),
                        new SqlParameter("p_coverage_delta", Types.DECIMAL),
                        new SqlParameter("p_effective_date", Types.DATE),
                        new SqlOutParameter("p_prorated_adjustment", Types.DECIMAL),
                        new SqlOutParameter("p_status_code", Types.INTEGER));
        SqlParameterSource in = new MapSqlParameterSource()
                .addValue("p_policy_id", policyId)
                .addValue("p_coverage_delta", coverageDelta, Types.DECIMAL)
                .addValue("p_effective_date", toSqlDate(effectiveDate));
        Map<String, Object> out = call.execute(in);
        int statusCode = ((Number) out.get("p_status_code")).intValue();
        if (statusCode == STATUS_NOT_ENDORSABLE) {
            throw new PolicyNotEndorsableException(
                    "Policy " + policyId + " is not endorsable (not found or not in active status)");
        }
        return (BigDecimal) out.get("p_prorated_adjustment");
    }

    /**
     * {@inheritDoc}
     *
     * <p>No stored procedure writes to {@code premium_adjustment_history}, so this method performs
     * the premium update and history insertion with direct {@link JdbcTemplate} statements inside a
     * single transaction: it reads the Policy's current {@code total_premium} (the previous premium),
     * computes the new premium by applying the signed adjustment, updates {@code policy.total_premium},
     * and inserts the audit row. The {@code @Transactional} boundary guarantees the premium update and
     * the history insertion either both take effect or neither does (Requirement 9.7).</p>
     */
    @Override
    @Transactional("jdbcTransactionManager")
    public void applyProrationAndRecordHistory(Long policyId, Long endorsementId, BigDecimal adjustmentAmount) {
        BigDecimal previousPremium = jdbcTemplate.queryForObject(
                "SELECT total_premium FROM policy WHERE policy_id = ?",
                BigDecimal.class,
                policyId);
        if (previousPremium == null) {
            previousPremium = BigDecimal.ZERO;
        }
        BigDecimal appliedAdjustment = adjustmentAmount == null ? BigDecimal.ZERO : adjustmentAmount;
        BigDecimal newPremium = previousPremium.add(appliedAdjustment);

        jdbcTemplate.update(
                "UPDATE policy SET total_premium = ? WHERE policy_id = ?",
                newPremium,
                policyId);

        jdbcTemplate.update(
                "INSERT INTO premium_adjustment_history "
                        + "(policy_id, endorsement_id, previous_premium, new_premium, adjustment_amount) "
                        + "VALUES (?, ?, ?, ?, ?)",
                policyId,
                endorsementId,
                previousPremium,
                newPremium,
                appliedAdjustment);
    }

    @Override
    public void regenerateRemainingInstallments(Long policyId, BigDecimal newTotalPremium) {
        SimpleJdbcCall call = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("sp_installment_regenerate_remaining")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(
                        new SqlParameter("p_policy_id", Types.BIGINT),
                        new SqlParameter("p_effective_date", Types.DATE),
                        new SqlParameter("p_new_remaining_total", Types.DECIMAL));
        SqlParameterSource in = new MapSqlParameterSource()
                .addValue("p_policy_id", policyId)
                .addValue("p_effective_date", toSqlDate(LocalDate.now()))
                .addValue("p_new_remaining_total", newTotalPremium, Types.DECIMAL);
        call.execute(in);
    }

    // ---------------------------------------------------------------------
    // Internal helpers
    // ---------------------------------------------------------------------

    private static Date toSqlDate(LocalDate value) {
        return value == null ? null : Date.valueOf(value);
    }

    @SuppressWarnings("unchecked")
    private static List<Endorsement> extractEndorsements(Object resultSet) {
        if (!(resultSet instanceof List)) {
            return new ArrayList<Endorsement>();
        }
        List<Endorsement> result = new ArrayList<Endorsement>();
        for (Object row : (List<?>) resultSet) {
            if (row instanceof Endorsement) {
                result.add((Endorsement) row);
            }
        }
        return result;
    }

    private static Optional<Endorsement> firstOrEmpty(List<Endorsement> rows) {
        return rows.isEmpty() ? Optional.<Endorsement>empty() : Optional.of(rows.get(0));
    }

    /** Maps an {@code endorsement} result-set row to a plain {@link Endorsement}. */
    private static final class EndorsementRowMapper implements RowMapper<Endorsement> {
        @Override
        public Endorsement mapRow(ResultSet rs, int rowNum) throws SQLException {
            Endorsement endorsement = new Endorsement();
            endorsement.setEndorsementId(getLong(rs, "endorsement_id"));
            endorsement.setPolicyId(getLong(rs, "policy_id"));
            endorsement.setDescription(rs.getString("description"));
            Date effectiveDate = rs.getDate("effective_date");
            endorsement.setEffectiveDate(effectiveDate == null ? null : effectiveDate.toLocalDate());
            endorsement.setPremiumAdjustment(rs.getBigDecimal("premium_adjustment"));
            Timestamp createdAt = rs.getTimestamp("created_at");
            endorsement.setCreatedAt(createdAt == null ? null : createdAt.toLocalDateTime());
            return endorsement;
        }
    }

    private static Long getLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : Long.valueOf(value);
    }
}
