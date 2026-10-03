package com.insurancepolicyadmin.dao.jdbc;

import com.insurancepolicyadmin.dao.PolicyDao;
import com.insurancepolicyadmin.model.plain.Coverage;
import com.insurancepolicyadmin.model.plain.Policy;
import com.insurancepolicyadmin.model.plain.PolicyStatusHistory;
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
import java.util.Optional;
import java.util.UUID;

/**
 * Spring JDBC implementation of {@link PolicyDao}.
 *
 * <p>Every persistence operation is performed by invoking a MySQL stored procedure through
 * {@link SimpleJdbcCall} over an {@link JdbcTemplate}. This class references no Hibernate
 * {@code Session}, no JPA {@code EntityManager}, and no {@code @Entity} type whatsoever,
 * satisfying the strict JDBC/stored-procedure boundary (Requirements 2.5, 7.1, 7.2, 7.5,
 * 8.5, 8.8).</p>
 */
@Repository
public class JdbcPolicyDao implements PolicyDao {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public JdbcPolicyDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Creates a Policy (initial status {@code quoted}) via {@code sp_policy_create}, persists each
     * supplied Coverage via {@code sp_coverage_create}, and records the supplied total premium via
     * {@code sp_policy_update}. The {@code sp_policy_create} procedure requires a unique
     * {@code policy_number}; one is generated here since it is not supplied by the service layer.
     */
    @Override
    public Long create(Long customerId, Long agentId, LocalDate termStartDate, LocalDate termEndDate,
                       String billingFrequency, BigDecimal totalPremium, List<Coverage> coverages) {
        SimpleJdbcCall createCall = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("sp_policy_create")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(
                        new SqlParameter("p_policy_number", Types.VARCHAR),
                        new SqlParameter("p_customer_id", Types.BIGINT),
                        new SqlParameter("p_agent_id", Types.BIGINT),
                        new SqlParameter("p_term_start_date", Types.DATE),
                        new SqlParameter("p_term_end_date", Types.DATE),
                        new SqlParameter("p_billing_frequency", Types.VARCHAR),
                        new SqlOutParameter("p_policy_id", Types.BIGINT));

        SqlParameterSource createIn = new MapSqlParameterSource()
                .addValue("p_policy_number", generatePolicyNumber())
                .addValue("p_customer_id", customerId)
                .addValue("p_agent_id", agentId)
                .addValue("p_term_start_date", toSqlDate(termStartDate))
                .addValue("p_term_end_date", toSqlDate(termEndDate))
                .addValue("p_billing_frequency", billingFrequency);

        Map<String, Object> createOut = createCall.execute(createIn);
        Long policyId = ((Number) createOut.get("p_policy_id")).longValue();

        if (coverages != null) {
            for (Coverage coverage : coverages) {
                createCoverage(policyId, coverage);
            }
        }

        if (totalPremium != null) {
            updateTotalPremium(policyId, agentId, termEndDate, billingFrequency, totalPremium);
        }

        return policyId;
    }

    @Override
    public Optional<Policy> findById(Long policyId) {
        SimpleJdbcCall call = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("sp_policy_read")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(new SqlParameter("p_policy_id", Types.BIGINT))
                .returningResultSet("policyRow", new PolicyRowMapper());

        Map<String, Object> out = call.execute(new MapSqlParameterSource()
                .addValue("p_policy_id", policyId));

        return firstOrEmpty(extractPolicies(out.get("policyRow")));
    }

    @Override
    public Optional<Policy> findByPolicyNumber(String policyNumber) {
        List<Policy> rows = jdbcTemplate.query(
                "SELECT policy_id, policy_number, customer_id, agent_id, underwriter_id, policy_status, "
                        + "term_start_date, term_end_date, billing_frequency, total_premium, created_at "
                        + "FROM policy WHERE policy_number = ?",
                new PolicyRowMapper(),
                policyNumber);
        return firstOrEmpty(rows);
    }

    @Override
    public List<Policy> search(Long customerId, String policyNumber, String policyStatus, Long agentId,
                               int page, int size) {
        SimpleJdbcCall call = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("sp_policy_search")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(
                        new SqlParameter("p_customer_id", Types.BIGINT),
                        new SqlParameter("p_policy_number", Types.VARCHAR),
                        new SqlParameter("p_status", Types.VARCHAR),
                        new SqlParameter("p_agent_id", Types.BIGINT))
                .returningResultSet("policyRows", new PolicyRowMapper());

        SqlParameterSource in = new MapSqlParameterSource()
                .addValue("p_customer_id", customerId, Types.BIGINT)
                .addValue("p_policy_number", policyNumber, Types.VARCHAR)
                .addValue("p_status", policyStatus, Types.VARCHAR)
                .addValue("p_agent_id", agentId, Types.BIGINT);

        Map<String, Object> out = call.execute(in);
        List<Policy> all = extractPolicies(out.get("policyRows"));
        return paginate(all, page, size);
    }

    @Override
    public int transitionStatus(Long policyId, String newStatus) {
        SimpleJdbcCall call = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("sp_policy_status_transition")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(
                        new SqlParameter("p_policy_id", Types.BIGINT),
                        new SqlParameter("p_new_status", Types.VARCHAR),
                        new SqlOutParameter("p_status_code", Types.INTEGER));

        Map<String, Object> out = call.execute(new MapSqlParameterSource()
                .addValue("p_policy_id", policyId)
                .addValue("p_new_status", newStatus));

        return ((Number) out.get("p_status_code")).intValue();
    }

    @Override
    public List<PolicyStatusHistory> findStatusHistory(Long policyId) {
        SimpleJdbcCall call = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("sp_policy_status_history_cursor")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(new SqlParameter("p_policy_id", Types.BIGINT))
                .returningResultSet("historyRows", new PolicyStatusHistoryRowMapper());

        Map<String, Object> out = call.execute(new MapSqlParameterSource()
                .addValue("p_policy_id", policyId));

        Object rows = out.get("historyRows");
        if (!(rows instanceof List)) {
            return new ArrayList<PolicyStatusHistory>();
        }
        List<PolicyStatusHistory> result = new ArrayList<PolicyStatusHistory>();
        for (Object row : (List<?>) rows) {
            if (row instanceof PolicyStatusHistory) {
                result.add((PolicyStatusHistory) row);
            }
        }
        return result;
    }


    /**
     * Inserts the initial {@code quoted} Policy_Status_History row (previous = {@code null}) for a
     * newly created Policy via {@code sp_policy_status_history_create} (Requirement 7.2).
     */
    @Override
    public void insertInitialStatusHistory(Long policyId) {
        SimpleJdbcCall call = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("sp_policy_status_history_create")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(
                        new SqlParameter("p_policy_id", Types.BIGINT),
                        new SqlParameter("p_previous_status", Types.VARCHAR),
                        new SqlParameter("p_new_status", Types.VARCHAR),
                        new SqlOutParameter("p_policy_status_history_id", Types.BIGINT));
        call.execute(new MapSqlParameterSource()
                .addValue("p_policy_id", policyId)
                .addValue("p_previous_status", null, Types.VARCHAR)
                .addValue("p_new_status", "quoted"));
    }

    /**
     * Records the approving Underwriter on an existing Policy via {@code sp_policy_update}
     * (Requirement 5.3), re-supplying the Policy's current mutable fields so only the
     * underwriter identifier changes.
     */
    @Override
    public void recordUnderwriter(Long policyId, Long underwriterId) {
        Policy current = findById(policyId)
                .orElseThrow(() -> new com.insurancepolicyadmin.exception.PolicyNotFoundException(policyId));
        SimpleJdbcCall call = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("sp_policy_update")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(
                        new SqlParameter("p_policy_id", Types.BIGINT),
                        new SqlParameter("p_agent_id", Types.BIGINT),
                        new SqlParameter("p_underwriter_id", Types.BIGINT),
                        new SqlParameter("p_term_end_date", Types.DATE),
                        new SqlParameter("p_billing_frequency", Types.VARCHAR),
                        new SqlParameter("p_total_premium", Types.DECIMAL));
        call.execute(new MapSqlParameterSource()
                .addValue("p_policy_id", policyId)
                .addValue("p_agent_id", current.getAgentId())
                .addValue("p_underwriter_id", underwriterId, Types.BIGINT)
                .addValue("p_term_end_date", toSqlDate(current.getTermEndDate()))
                .addValue("p_billing_frequency", current.getBillingFrequency())
                .addValue("p_total_premium", current.getTotalPremium()));
    }

    // ---------------------------------------------------------------------
    // Internal helpers (stored-procedure invocations only)
    // ---------------------------------------------------------------------

    private void createCoverage(Long policyId, Coverage coverage) {
        SimpleJdbcCall call = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("sp_coverage_create")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(
                        new SqlParameter("p_policy_id", Types.BIGINT),
                        new SqlParameter("p_coverage_type_id", Types.BIGINT),
                        new SqlParameter("p_coverage_limit", Types.DECIMAL),
                        new SqlOutParameter("p_coverage_id", Types.BIGINT));

        call.execute(new MapSqlParameterSource()
                .addValue("p_policy_id", policyId)
                .addValue("p_coverage_type_id", coverage.getCoverageTypeId())
                .addValue("p_coverage_limit", coverage.getCoverageLimit()));
    }

    private void updateTotalPremium(Long policyId, Long agentId, LocalDate termEndDate,
                                    String billingFrequency, BigDecimal totalPremium) {
        SimpleJdbcCall call = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("sp_policy_update")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(
                        new SqlParameter("p_policy_id", Types.BIGINT),
                        new SqlParameter("p_agent_id", Types.BIGINT),
                        new SqlParameter("p_underwriter_id", Types.BIGINT),
                        new SqlParameter("p_term_end_date", Types.DATE),
                        new SqlParameter("p_billing_frequency", Types.VARCHAR),
                        new SqlParameter("p_total_premium", Types.DECIMAL));

        call.execute(new MapSqlParameterSource()
                .addValue("p_policy_id", policyId)
                .addValue("p_agent_id", agentId)
                .addValue("p_underwriter_id", null, Types.BIGINT)
                .addValue("p_term_end_date", toSqlDate(termEndDate))
                .addValue("p_billing_frequency", billingFrequency)
                .addValue("p_total_premium", totalPremium));
    }

    private String generatePolicyNumber() {
        return "POL-" + UUID.randomUUID().toString().replace("-", "").substring(0, 14).toUpperCase();
    }

    private static Date toSqlDate(LocalDate value) {
        return value == null ? null : Date.valueOf(value);
    }

    @SuppressWarnings("unchecked")
    private static List<Policy> extractPolicies(Object resultSet) {
        if (!(resultSet instanceof List)) {
            return new ArrayList<Policy>();
        }
        List<Policy> result = new ArrayList<Policy>();
        for (Object row : (List<?>) resultSet) {
            if (row instanceof Policy) {
                result.add((Policy) row);
            }
        }
        return result;
    }

    private static Optional<Policy> firstOrEmpty(List<Policy> rows) {
        return rows.isEmpty() ? Optional.<Policy>empty() : Optional.of(rows.get(0));
    }

    private static List<Policy> paginate(List<Policy> all, int page, int size) {
        if (size <= 0 || all.isEmpty()) {
            return new ArrayList<Policy>();
        }
        int from = Math.max(page, 0) * size;
        if (from >= all.size()) {
            return new ArrayList<Policy>();
        }
        int to = Math.min(from + size, all.size());
        return new ArrayList<Policy>(all.subList(from, to));
    }

    /** Maps a {@code policy} result-set row to a plain {@link Policy}. */
    private static final class PolicyRowMapper implements RowMapper<Policy> {
        @Override
        public Policy mapRow(ResultSet rs, int rowNum) throws SQLException {
            Policy policy = new Policy();
            policy.setPolicyId(getLong(rs, "policy_id"));
            policy.setPolicyNumber(rs.getString("policy_number"));
            policy.setCustomerId(getLong(rs, "customer_id"));
            policy.setAgentId(getLong(rs, "agent_id"));
            policy.setUnderwriterId(getLong(rs, "underwriter_id"));
            policy.setPolicyStatus(rs.getString("policy_status"));
            Date termStart = rs.getDate("term_start_date");
            policy.setTermStartDate(termStart == null ? null : termStart.toLocalDate());
            Date termEnd = rs.getDate("term_end_date");
            policy.setTermEndDate(termEnd == null ? null : termEnd.toLocalDate());
            policy.setBillingFrequency(rs.getString("billing_frequency"));
            policy.setTotalPremium(rs.getBigDecimal("total_premium"));
            Timestamp createdAt = rs.getTimestamp("created_at");
            policy.setCreatedAt(createdAt == null ? null : createdAt.toLocalDateTime());
            return policy;
        }
    }

    /** Maps a {@code policy_status_history} result-set row to a plain {@link PolicyStatusHistory}. */
    private static final class PolicyStatusHistoryRowMapper implements RowMapper<PolicyStatusHistory> {
        @Override
        public PolicyStatusHistory mapRow(ResultSet rs, int rowNum) throws SQLException {
            PolicyStatusHistory history = new PolicyStatusHistory();
            history.setPolicyStatusHistoryId(getLong(rs, "policy_status_history_id"));
            history.setPolicyId(getLong(rs, "policy_id"));
            history.setPreviousStatus(rs.getString("previous_status"));
            history.setNewStatus(rs.getString("new_status"));
            Timestamp transitionedAt = rs.getTimestamp("transitioned_at");
            history.setTransitionedAt(transitionedAt == null ? null : transitionedAt.toLocalDateTime());
            return history;
        }
    }

    private static Long getLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : Long.valueOf(value);
    }
}
