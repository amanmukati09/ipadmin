package com.insurancepolicyadmin.dao.jdbc;

import com.insurancepolicyadmin.dao.PremiumDao;
import com.insurancepolicyadmin.model.plain.Installment;
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
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Spring JDBC implementation of {@link PremiumDao}.
 *
 * <p>Every persistence operation is performed by invoking a MySQL stored procedure through
 * {@link SimpleJdbcCall} over a {@link JdbcTemplate}. This class references no Hibernate
 * {@code Session}, no JPA {@code EntityManager}, and no {@code @Entity} type whatsoever,
 * satisfying the strict JDBC/stored-procedure boundary (Requirements 2.5, 10.1, 10.6, 10.8).</p>
 *
 * <p>{@link #generateInstallments(Long, BigDecimal, String, LocalDate)} first creates the
 * {@code premium_schedule} row via {@code sp_premium_schedule_create}, then populates its
 * {@code installment} rows via {@code sp_premium_schedule_generate_installments} (which reads the
 * schedule it just created). Both procedure calls run inside a single {@code @Transactional}
 * boundary so the schedule and its installments are committed together or not at all.
 * {@link #findScheduleByPolicyId(Long)} reads the Policy's installments through the cursor-based
 * {@code sp_premium_schedule_installments_cursor}, which returns rows ordered by due date
 * ascending.</p>
 */
@Repository
public class JdbcPremiumDao implements PremiumDao {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public JdbcPremiumDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Creates the Premium_Schedule row and then generates its Installments. The two stored
     * procedure invocations are wrapped in a single transaction: {@code sp_premium_schedule_create}
     * inserts the schedule and {@code sp_premium_schedule_generate_installments} looks that schedule
     * up by Policy id before inserting the floor-rounded Installments (Requirements 10.1, 10.6).</p>
     */
    @Override
    @Transactional("jdbcTransactionManager")
    public void generateInstallments(Long policyId, BigDecimal totalPremium, String billingFrequency,
                                     LocalDate termStartDate) {
        SimpleJdbcCall createCall = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("sp_premium_schedule_create")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(
                        new SqlParameter("p_policy_id", Types.BIGINT),
                        new SqlParameter("p_billing_frequency", Types.VARCHAR),
                        new SqlParameter("p_total_premium", Types.DECIMAL),
                        new SqlOutParameter("p_premium_schedule_id", Types.BIGINT));
        SqlParameterSource createIn = new MapSqlParameterSource()
                .addValue("p_policy_id", policyId)
                .addValue("p_billing_frequency", billingFrequency, Types.VARCHAR)
                .addValue("p_total_premium", totalPremium, Types.DECIMAL);
        createCall.execute(createIn);

        SimpleJdbcCall generateCall = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("sp_premium_schedule_generate_installments")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(
                        new SqlParameter("p_policy_id", Types.BIGINT),
                        new SqlParameter("p_total_premium", Types.DECIMAL),
                        new SqlParameter("p_billing_frequency", Types.VARCHAR),
                        new SqlParameter("p_term_start_date", Types.DATE));
        SqlParameterSource generateIn = new MapSqlParameterSource()
                .addValue("p_policy_id", policyId)
                .addValue("p_total_premium", totalPremium, Types.DECIMAL)
                .addValue("p_billing_frequency", billingFrequency, Types.VARCHAR)
                .addValue("p_term_start_date", toSqlDate(termStartDate));
        generateCall.execute(generateIn);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Delegates to the cursor-based {@code sp_premium_schedule_installments_cursor}, which
     * returns the Policy's Installment rows ordered by due date ascending (Requirement 10.8).</p>
     */
    @Override
    public List<Installment> findScheduleByPolicyId(Long policyId) {
        SimpleJdbcCall call = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("sp_premium_schedule_installments_cursor")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(new SqlParameter("p_policy_id", Types.BIGINT))
                .returningResultSet("installmentRows", new InstallmentRowMapper());
        Map<String, Object> out = call.execute(new MapSqlParameterSource()
                .addValue("p_policy_id", policyId));
        return extractInstallments(out.get("installmentRows"));
    }

    // ---------------------------------------------------------------------
    // Internal helpers
    // ---------------------------------------------------------------------

    private static Date toSqlDate(LocalDate value) {
        return value == null ? null : Date.valueOf(value);
    }

    @SuppressWarnings("unchecked")
    private static List<Installment> extractInstallments(Object resultSet) {
        if (!(resultSet instanceof List)) {
            return new ArrayList<Installment>();
        }
        List<Installment> result = new ArrayList<Installment>();
        for (Object row : (List<?>) resultSet) {
            if (row instanceof Installment) {
                result.add((Installment) row);
            }
        }
        return result;
    }

    /** Maps an {@code installment} result-set row to a plain {@link Installment}. */
    private static final class InstallmentRowMapper implements RowMapper<Installment> {
        @Override
        public Installment mapRow(ResultSet rs, int rowNum) throws SQLException {
            Installment installment = new Installment();
            installment.setInstallmentId(getLong(rs, "installment_id"));
            installment.setPremiumScheduleId(getLong(rs, "premium_schedule_id"));
            Date dueDate = rs.getDate("due_date");
            installment.setDueDate(dueDate == null ? null : dueDate.toLocalDate());
            installment.setAmountDue(rs.getBigDecimal("amount_due"));
            installment.setAmountPaid(rs.getBigDecimal("amount_paid"));
            installment.setInstallmentStatus(rs.getString("installment_status"));
            return installment;
        }
    }

    private static Long getLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : Long.valueOf(value);
    }
}
