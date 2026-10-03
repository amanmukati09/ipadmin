package com.insurancepolicyadmin.dao.jdbc;

import com.insurancepolicyadmin.dao.ReportDao;
import com.insurancepolicyadmin.model.plain.Policy;
import com.insurancepolicyadmin.model.plain.RenewalNotice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.CallableStatementCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Spring JDBC implementation of {@link ReportDao}.
 *
 * <p>Every operation is performed by invoking a MySQL stored procedure through
 * {@link SimpleJdbcCall} or {@link JdbcTemplate#execute(org.springframework.jdbc.core.CallableStatementCallback)}
 * over a single shared {@link JdbcTemplate}. This class references no Hibernate
 * {@code Session}, no JPA {@code EntityManager}, and no {@code @Entity} type
 * whatsoever, satisfying the strict JDBC/stored-procedure boundary
 * (Requirements 2.5, 12.1, 12.3, 12.4, 13.1, 13.4, 13.6, 13.9).</p>
 *
 * <p>Wired procedures:
 * <ul>
 *   <li>{@code sp_renewal_notice_generate} &mdash; generate/duplicate-check (status codes
 *       0 ok, 1 ineligible, 2 duplicate, 3 error), with the resulting notice resolved via
 *       {@code sp_renewal_notice_get_by_id} / {@code sp_renewal_notice_list_by_policy}.</li>
 *   <li>{@code sp_report_expiring_policies(p_day_count)}</li>
 *   <li>{@code sp_report_overdue_installments}</li>
 *   <li>{@code sp_report_agent_book_of_business(p_agent_id)} &mdash; a combined multi-result-set
 *       procedure (policy rows + summary row) consumed defensively, see
 *       {@link #findAgentBookOfBusiness(Long)}.</li>
 * </ul>
 * </p>
 */
@Repository
public class JdbcReportDao implements ReportDao {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcReportDao.class);

    /** Status codes returned by {@code sp_renewal_notice_generate}. */
    private static final int STATUS_OK = 0;
    private static final int STATUS_INELIGIBLE = 1;
    private static final int STATUS_DUPLICATE = 2;

    /** Reporting procedures cap results at 1000 rows per the interface contract. */
    private static final int MAX_REPORT_ROWS = 1000;

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public JdbcReportDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // ---------------------------------------------------------------------
    // Renewal notice generation (sp_renewal_notice_generate)
    // ---------------------------------------------------------------------

    @Override
    public RenewalNotice generateRenewalNotice(Long policyId, LocalDate termEndDate) {
        if (policyId == null) {
            throw new IllegalArgumentException("policyId must not be null");
        }
        if (termEndDate == null) {
            throw new IllegalArgumentException("termEndDate must not be null");
        }

        // The procedure takes a lookahead window in days and reads the policy's own
        // term_end_date; derive the window from the supplied term end date (today-based),
        // clamped to a non-negative value. The procedure itself validates eligibility.
        long lookahead = ChronoUnit.DAYS.between(LocalDate.now(), termEndDate);
        int lookaheadDays = lookahead < 0L ? 0 : (lookahead > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) lookahead);

        SimpleJdbcCall call = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("sp_renewal_notice_generate")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(
                        new SqlParameter("p_policy_id", Types.BIGINT),
                        new SqlParameter("p_lookahead_days", Types.INTEGER),
                        new SqlOutParameter("p_renewal_notice_id", Types.BIGINT),
                        new SqlOutParameter("p_status_code", Types.INTEGER));

        SqlParameterSource in = new MapSqlParameterSource()
                .addValue("p_policy_id", policyId, Types.BIGINT)
                .addValue("p_lookahead_days", lookaheadDays, Types.INTEGER);

        Map<String, Object> out = call.execute(in);

        Integer statusCode = asInteger(out.get("p_status_code"));
        Long renewalNoticeId = asLong(out.get("p_renewal_notice_id"));
        int status = statusCode == null ? STATUS_INELIGIBLE : statusCode.intValue();

        LOGGER.debug("sp_renewal_notice_generate policyId={} statusCode={} renewalNoticeId={}",
                policyId, status, renewalNoticeId);

        if (status == STATUS_OK && renewalNoticeId != null) {
            RenewalNotice generated = findRenewalNoticeById(renewalNoticeId);
            if (generated != null) {
                return generated;
            }
        }
        if (status == STATUS_DUPLICATE) {
            // Duplicate: the procedure did not emit the pre-existing id; resolve it from
            // the policy's notices by matching the term-end-date snapshot.
            RenewalNotice existing = findExistingRenewalNotice(policyId, termEndDate);
            if (existing != null) {
                return existing;
            }
        }
        // Ineligible (1), error (3), or an unexpectedly unresolved notice: no record to return.
        return null;
    }

    private RenewalNotice findRenewalNoticeById(Long renewalNoticeId) {
        SimpleJdbcCall call = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("sp_renewal_notice_get_by_id")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(new SqlParameter("p_renewal_notice_id", Types.BIGINT))
                .returningResultSet("noticeRows", new RenewalNoticeRowMapper());
        Map<String, Object> out = call.execute(new MapSqlParameterSource()
                .addValue("p_renewal_notice_id", renewalNoticeId, Types.BIGINT));
        List<RenewalNotice> rows = extract(out.get("noticeRows"), RenewalNotice.class);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private RenewalNotice findExistingRenewalNotice(Long policyId, LocalDate termEndDate) {
        SimpleJdbcCall call = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("sp_renewal_notice_list_by_policy")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(new SqlParameter("p_policy_id", Types.BIGINT))
                .returningResultSet("noticeRows", new RenewalNoticeRowMapper());
        Map<String, Object> out = call.execute(new MapSqlParameterSource()
                .addValue("p_policy_id", policyId, Types.BIGINT));
        List<RenewalNotice> rows = extract(out.get("noticeRows"), RenewalNotice.class);
        for (RenewalNotice notice : rows) {
            if (termEndDate.equals(notice.getTermEndDateSnapshot())) {
                return notice;
            }
        }
        // Fall back to the most recent notice (list is ordered generated_at DESC).
        return rows.isEmpty() ? null : rows.get(0);
    }

    // ---------------------------------------------------------------------
    // Expiring policies (sp_report_expiring_policies)
    // ---------------------------------------------------------------------

    @Override
    public List<Policy> findExpiringPolicies(int dayCount) {
        if (dayCount <= 0) {
            return new ArrayList<Policy>();
        }
        SimpleJdbcCall call = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("sp_report_expiring_policies")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(new SqlParameter("p_day_count", Types.INTEGER))
                .returningResultSet("policyRows", new PolicyReportRowMapper());
        Map<String, Object> out = call.execute(new MapSqlParameterSource()
                .addValue("p_day_count", dayCount, Types.INTEGER));
        return cap(extract(out.get("policyRows"), Policy.class));
    }

    // ---------------------------------------------------------------------
    // Overdue installments (sp_report_overdue_installments)
    // ---------------------------------------------------------------------

    @Override
    public List<OverdueInstallment> findOverdueInstallments() {
        SimpleJdbcCall call = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("sp_report_overdue_installments")
                .withoutProcedureColumnMetaDataAccess()
                .returningResultSet("overdueRows", new OverdueInstallmentRowMapper());
        Map<String, Object> out = call.execute(new MapSqlParameterSource());
        return cap(extract(out.get("overdueRows"), OverdueInstallment.class));
    }

    // ---------------------------------------------------------------------
    // Agent book of business (sp_report_agent_book_of_business)
    //
    // This procedure returns MULTIPLE result sets: a set of policy rows followed
    // by a one-row summary. We consume ALL result sets defensively via
    // CallableStatement.getMoreResults(), classifying each by the columns it
    // exposes rather than assuming a fixed order or count, and collect only the
    // policy-row result set(s).
    // ---------------------------------------------------------------------

    @Override
    public List<Policy> findAgentBookOfBusiness(final Long agentId) {
        if (agentId == null) {
            return new ArrayList<Policy>();
        }
        List<Policy> policies = jdbcTemplate.execute(
                "{call sp_report_agent_book_of_business(?)}",
                new CallableStatementCallback<List<Policy>>() {
                    @Override
                    public List<Policy> doInCallableStatement(CallableStatement cs)
                            throws SQLException, DataAccessException {
                        cs.setLong(1, agentId.longValue());
                        List<Policy> collected = new ArrayList<Policy>();
                        PolicyReportRowMapper mapper = new PolicyReportRowMapper();

                        boolean hasResultSet = cs.execute();
                        // Defensive loop over every result set the procedure emits.
                        while (true) {
                            if (hasResultSet) {
                                ResultSet rs = cs.getResultSet();
                                if (rs != null) {
                                    try {
                                        if (isPolicyResultSet(rs.getMetaData())) {
                                            int rowNum = 0;
                                            while (rs.next()) {
                                                collected.add(mapper.mapRow(rs, rowNum++));
                                            }
                                        }
                                    } finally {
                                        rs.close();
                                    }
                                }
                            } else {
                                // No result set here; an update count of -1 with no more
                                // results signals the end of the procedure output.
                                if (cs.getUpdateCount() == -1) {
                                    break;
                                }
                            }
                            hasResultSet = cs.getMoreResults();
                        }
                        return collected;
                    }
                });
        return cap(policies);
    }

    /**
     * Classifies a result set as the policy-detail set (versus the summary set) by the
     * columns it exposes, so consumption never relies on positional ordering.
     */
    private static boolean isPolicyResultSet(ResultSetMetaData meta) throws SQLException {
        Set<String> columns = columnNames(meta);
        // The summary result set exposes policy_count; the detail set exposes policy_id.
        return columns.contains("policy_id") && !columns.contains("policy_count");
    }

    private static Set<String> columnNames(ResultSetMetaData meta) throws SQLException {
        Set<String> names = new LinkedHashSet<String>();
        int count = meta.getColumnCount();
        for (int i = 1; i <= count; i++) {
            String label = meta.getColumnLabel(i);
            if (label == null || label.isEmpty()) {
                label = meta.getColumnName(i);
            }
            if (label != null) {
                names.add(label.toLowerCase());
            }
        }
        return names;
    }

    // ---------------------------------------------------------------------
    // Shared helpers
    // ---------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private static <T> List<T> extract(Object resultSet, Class<T> type) {
        if (!(resultSet instanceof List)) {
            return new ArrayList<T>();
        }
        List<T> result = new ArrayList<T>();
        for (Object row : (List<?>) resultSet) {
            if (type.isInstance(row)) {
                result.add((T) row);
            }
        }
        return result;
    }

    private static <T> List<T> cap(List<T> rows) {
        if (rows.size() <= MAX_REPORT_ROWS) {
            return rows;
        }
        return new ArrayList<T>(rows.subList(0, MAX_REPORT_ROWS));
    }

    private static Long asLong(Object value) {
        return value instanceof Number ? Long.valueOf(((Number) value).longValue()) : null;
    }

    private static Integer asInteger(Object value) {
        return value instanceof Number ? Integer.valueOf(((Number) value).intValue()) : null;
    }

    private static Long getLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : Long.valueOf(value);
    }

    private static boolean hasColumn(ResultSet rs, String column) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        int count = meta.getColumnCount();
        for (int i = 1; i <= count; i++) {
            String label = meta.getColumnLabel(i);
            if (label == null || label.isEmpty()) {
                label = meta.getColumnName(i);
            }
            if (label != null && label.equalsIgnoreCase(column)) {
                return true;
            }
        }
        return false;
    }

    // ---------------------------------------------------------------------
    // Row mappers
    // ---------------------------------------------------------------------

    /**
     * Maps a reporting policy row to a plain {@link Policy}. Reporting procedures expose
     * differing column subsets, so each optional column is read only when present.
     */
    private static final class PolicyReportRowMapper implements RowMapper<Policy> {
        @Override
        public Policy mapRow(ResultSet rs, int rowNum) throws SQLException {
            Policy policy = new Policy();
            policy.setPolicyId(getLong(rs, "policy_id"));
            policy.setPolicyNumber(rs.getString("policy_number"));
            policy.setCustomerId(getLong(rs, "customer_id"));
            if (hasColumn(rs, "agent_id")) {
                policy.setAgentId(getLong(rs, "agent_id"));
            }
            if (hasColumn(rs, "underwriter_id")) {
                policy.setUnderwriterId(getLong(rs, "underwriter_id"));
            }
            policy.setPolicyStatus(rs.getString("policy_status"));
            if (hasColumn(rs, "term_start_date")) {
                Date termStart = rs.getDate("term_start_date");
                policy.setTermStartDate(termStart == null ? null : termStart.toLocalDate());
            }
            Date termEnd = rs.getDate("term_end_date");
            policy.setTermEndDate(termEnd == null ? null : termEnd.toLocalDate());
            if (hasColumn(rs, "billing_frequency")) {
                policy.setBillingFrequency(rs.getString("billing_frequency"));
            }
            policy.setTotalPremium(rs.getBigDecimal("total_premium"));
            if (hasColumn(rs, "created_at")) {
                Timestamp createdAt = rs.getTimestamp("created_at");
                policy.setCreatedAt(createdAt == null ? null : createdAt.toLocalDateTime());
            }
            return policy;
        }
    }

    /** Maps a {@code renewal_notice} row to a plain {@link RenewalNotice}. */
    private static final class RenewalNoticeRowMapper implements RowMapper<RenewalNotice> {
        @Override
        public RenewalNotice mapRow(ResultSet rs, int rowNum) throws SQLException {
            RenewalNotice notice = new RenewalNotice();
            notice.setRenewalNoticeId(getLong(rs, "renewal_notice_id"));
            notice.setPolicyId(getLong(rs, "policy_id"));
            Timestamp generatedAt = rs.getTimestamp("generated_at");
            notice.setGeneratedAt(generatedAt == null ? null : generatedAt.toLocalDateTime());
            Date snapshot = rs.getDate("term_end_date_snapshot");
            notice.setTermEndDateSnapshot(snapshot == null ? null : snapshot.toLocalDate());
            return notice;
        }
    }

    /** Maps an overdue-installment reporting row to a plain {@link OverdueInstallment}. */
    private static final class OverdueInstallmentRowMapper implements RowMapper<OverdueInstallment> {
        @Override
        public OverdueInstallment mapRow(ResultSet rs, int rowNum) throws SQLException {
            Long installmentId = getLong(rs, "installment_id");
            Long policyId = getLong(rs, "policy_id");
            Long customerId = getLong(rs, "customer_id");
            Date dueDate = rs.getDate("due_date");
            LocalDate due = dueDate == null ? null : dueDate.toLocalDate();
            BigDecimal amountDue = rs.getBigDecimal("amount_due");
            BigDecimal amountPaid = rs.getBigDecimal("amount_paid");
            return new OverdueInstallment(installmentId, policyId, customerId, due, amountDue, amountPaid);
        }
    }
}
