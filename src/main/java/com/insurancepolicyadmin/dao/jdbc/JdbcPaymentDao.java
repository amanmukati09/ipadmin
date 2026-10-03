package com.insurancepolicyadmin.dao.jdbc;

import com.insurancepolicyadmin.dao.PaymentDao;
import com.insurancepolicyadmin.model.plain.Payment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Repository;

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

/**
 * Spring JDBC implementation of {@link PaymentDao}.
 *
 * <p>Every persistence operation is performed by invoking a MySQL stored procedure through
 * {@link SimpleJdbcCall} over a {@link JdbcTemplate}, following the same conventions as the
 * sibling {@code JdbcPolicyDao}/{@code JdbcEndorsementDao}: {@code @Repository} stereotype,
 * constructor-injected {@code JdbcTemplate}, {@code SimpleJdbcCall} with named binding and
 * explicitly declared parameters, and inner {@link RowMapper}s. This class references no
 * Hibernate {@code Session}, no JPA {@code EntityManager}, and no {@code @Entity} type
 * whatsoever, satisfying the strict JDBC/stored-procedure boundary (Requirements 2.5, 11.1,
 * 11.2, 11.3, 11.8, 11.9).</p>
 */
@Repository
public class JdbcPaymentDao implements PaymentDao {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public JdbcPaymentDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Delegates to {@code sp_payment_apply}, which allocates the payment to the oldest unpaid
     * Installment first (due-date order) within its own transaction. The procedure signals
     * {@code p_status_code = 1} when the amount exceeds the remaining unpaid balance (including a
     * zero remaining balance), in which case no Payment row is created and {@code p_payment_id} is
     * {@code NULL}. The OUT values are surfaced through the technology-neutral
     * {@link PaymentDao.PaymentResult} contract, where status code {@code 1} corresponds to
     * {@link PaymentDao.PaymentResult#STATUS_OVERPAYMENT} (Requirements 11.1, 11.2, 11.3, 11.7).</p>
     */
    @Override
    public PaymentResult applyPayment(Long policyId, BigDecimal amount, LocalDate paymentDate) {
        SimpleJdbcCall call = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("sp_payment_apply")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(
                        new SqlParameter("p_policy_id", Types.BIGINT),
                        new SqlParameter("p_amount", Types.DECIMAL),
                        new SqlParameter("p_payment_date", Types.DATE),
                        new SqlOutParameter("p_payment_id", Types.BIGINT),
                        new SqlOutParameter("p_status_code", Types.INTEGER));
        SqlParameterSource in = new MapSqlParameterSource()
                .addValue("p_policy_id", policyId)
                .addValue("p_amount", amount, Types.DECIMAL)
                .addValue("p_payment_date", toSqlDate(paymentDate));
        Map<String, Object> out = call.execute(in);
        int statusCode = ((Number) out.get("p_status_code")).intValue();
        Object paymentIdValue = out.get("p_payment_id");
        Long paymentId = paymentIdValue == null ? null : ((Number) paymentIdValue).longValue();
        return new PaymentResult(paymentId, statusCode);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Delegates to {@code sp_payment_history_cursor}, whose single result set is ordered by
     * payment date descending (Requirements 11.8, 11.9).</p>
     */
    @Override
    public List<Payment> findHistoryByPolicyId(Long policyId) {
        SimpleJdbcCall call = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("sp_payment_history_cursor")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(new SqlParameter("p_policy_id", Types.BIGINT))
                .returningResultSet("paymentRows", new PaymentRowMapper());
        Map<String, Object> out = call.execute(new MapSqlParameterSource()
                .addValue("p_policy_id", policyId));
        return extractPayments(out.get("paymentRows"));
    }

    // ---------------------------------------------------------------------
    // Internal helpers
    // ---------------------------------------------------------------------

    private static Date toSqlDate(LocalDate value) {
        return value == null ? null : Date.valueOf(value);
    }

    @SuppressWarnings("unchecked")
    private static List<Payment> extractPayments(Object resultSet) {
        if (!(resultSet instanceof List)) {
            return new ArrayList<Payment>();
        }
        List<Payment> result = new ArrayList<Payment>();
        for (Object row : (List<?>) resultSet) {
            if (row instanceof Payment) {
                result.add((Payment) row);
            }
        }
        return result;
    }

    /** Maps a {@code payment} result-set row to a plain {@link Payment}. */
    private static final class PaymentRowMapper implements RowMapper<Payment> {
        @Override
        public Payment mapRow(ResultSet rs, int rowNum) throws SQLException {
            Payment payment = new Payment();
            payment.setPaymentId(getLong(rs, "payment_id"));
            payment.setPolicyId(getLong(rs, "policy_id"));
            payment.setPaymentAmount(rs.getBigDecimal("payment_amount"));
            Date paymentDate = rs.getDate("payment_date");
            payment.setPaymentDate(paymentDate == null ? null : paymentDate.toLocalDate());
            Timestamp createdAt = rs.getTimestamp("created_at");
            payment.setCreatedAt(createdAt == null ? null : createdAt.toLocalDateTime());
            return payment;
        }
    }

    private static Long getLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : Long.valueOf(value);
    }
}
