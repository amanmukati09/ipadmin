package com.insurancepolicyadmin.service.impl;

import com.insurancepolicyadmin.dao.AgentDao;
import com.insurancepolicyadmin.dao.ReportDao;
import com.insurancepolicyadmin.exception.AgentNotFoundException;
import com.insurancepolicyadmin.exception.ReportExportException;
import com.insurancepolicyadmin.exception.ValidationException;
import com.insurancepolicyadmin.model.plain.Policy;
import com.insurancepolicyadmin.model.plain.RenewalNotice;
import com.insurancepolicyadmin.dto.response.OverdueInstallmentResponse;
import com.insurancepolicyadmin.dto.response.PolicySummaryResponse;
import com.insurancepolicyadmin.dto.response.RenewalNoticeResponse;
import com.insurancepolicyadmin.service.ReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

/**
 * Default {@link ReportService} implementation.
 *
 * <p>Business rules (lookahead/day-count range validation, Agent existence checks, empty-result
 * handling) live here; the actual reporting queries and Renewal_Notice persistence are delegated to
 * {@link ReportDao} (its {@code JdbcReportDao} implementation invokes MySQL stored procedures via
 * Spring JDBC). Agent existence is verified through {@link AgentDao}. The Renewal_Notice generation
 * path mutates state and therefore runs under the Spring-JDBC-backed {@code DataSourceTransactionManager}
 * (qualifier {@code jdbcTransactionManager}); the pure read paths run read-only under the same manager
 * so that each stored-procedure call commits/rolls back atomically (Requirement 12.5).</p>
 *
 * <p>{@link #exportBookOfBusinessReport(Long)} intentionally collapses three unrelated checked
 * exceptions ({@link IOException}, {@link ParseException}, {@link MessagingException}) into a single
 * {@code try} block, rethrowing each as the unchecked {@link ReportExportException} (design.md
 * "Multi-checked-exception try block", Requirement 13.8).</p>
 */
@Service
public class ReportServiceImpl implements ReportService {

    private static final Logger LOG = LoggerFactory.getLogger(ReportServiceImpl.class);

    private static final int MIN_LOOKAHEAD_DAYS = 1;
    private static final int MAX_LOOKAHEAD_DAYS = 365;

    private final ReportDao reportDao;
    private final AgentDao agentDao;

    @Autowired
    public ReportServiceImpl(ReportDao reportDao, AgentDao agentDao) {
        this.reportDao = reportDao;
        this.agentDao = agentDao;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Validates the lookahead window (Requirement 12.2), resolves the active Policies expiring
     * within the window via {@link ReportDao#findExpiringPolicies(int)}, then generates (or resolves
     * the pre-existing) Renewal_Notice for each via {@link ReportDao#generateRenewalNotice(Long, LocalDate)}.
     * The DAO/stored-procedure handles duplicate-cycle detection, returning the pre-existing notice for a
     * Policy whose current renewal cycle already has one (Requirement 12.4); such notices are surfaced in
     * the returned list exactly like freshly generated ones. Policies the procedure deems ineligible
     * (returning no notice) are skipped.</p>
     */
    @Override
    @Transactional("jdbcTransactionManager")
    public List<RenewalNoticeResponse> generateRenewalNotices(int lookaheadDays) {
        if (lookaheadDays < MIN_LOOKAHEAD_DAYS || lookaheadDays > MAX_LOOKAHEAD_DAYS) {
            throw new ValidationException(
                    "lookaheadDays must be between " + MIN_LOOKAHEAD_DAYS + " and " + MAX_LOOKAHEAD_DAYS
                            + " inclusive, but was " + lookaheadDays);
        }
        List<Policy> expiring = reportDao.findExpiringPolicies(lookaheadDays);
        List<RenewalNoticeResponse> responses = new ArrayList<>();
        for (Policy policy : expiring) {
            RenewalNotice notice = reportDao.generateRenewalNotice(policy.getPolicyId(), policy.getTermEndDate());
            if (notice != null) {
                responses.add(toRenewalNoticeResponse(notice));
            }
        }
        LOG.debug("Generated/resolved {} renewal notice(s) for lookaheadDays={}", responses.size(), lookaheadDays);
        return responses;
    }

    /**
     * {@inheritDoc}
     *
     * <p>A zero or negative {@code dayCount} is not an error (Requirement 13.2): the DAO returns an
     * empty list for it, which is mapped through unchanged (Requirement 13.3).</p>
     */
    @Override
    @Transactional("jdbcTransactionManager")
    public List<PolicySummaryResponse> getExpiringPolicies(int dayCount) {
        List<Policy> policies = reportDao.findExpiringPolicies(dayCount);
        List<PolicySummaryResponse> responses = new ArrayList<>(policies.size());
        for (Policy policy : policies) {
            responses.add(toPolicySummaryResponse(policy));
        }
        return responses;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns an empty list when no Installment is overdue (Requirement 13.5).</p>
     */
    @Override
    @Transactional("jdbcTransactionManager")
    public List<OverdueInstallmentResponse> getOverduePayments() {
        List<ReportDao.OverdueInstallment> overdue = reportDao.findOverdueInstallments();
        List<OverdueInstallmentResponse> responses = new ArrayList<>(overdue.size());
        LocalDate today = LocalDate.now();
        for (ReportDao.OverdueInstallment row : overdue) {
            responses.add(toOverdueInstallmentResponse(row, today));
        }
        return responses;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Rejects an unknown Agent with {@link AgentNotFoundException} (Requirement 13.8); an existing
     * Agent with no attributed Policies yields an empty list rather than an error (Requirement 13.7).</p>
     */
    @Override
    @Transactional("hibernateTransactionManager")
    public List<PolicySummaryResponse> getAgentBookOfBusiness(Long agentId) {
        requireAgent(agentId);
        List<Policy> policies = reportDao.findAgentBookOfBusiness(agentId);
        List<PolicySummaryResponse> responses = new ArrayList<>(policies.size());
        for (Policy policy : policies) {
            responses.add(toPolicySummaryResponse(policy));
        }
        return responses;
    }

    /**
     * {@inheritDoc}
     *
     * <p><strong>INTENTIONAL CHECKED-EXCEPTION SMELL (Requirement 13.8):</strong> the export logic
     * genuinely exercises three unrelated checked-exception types &mdash; {@link IOException} (writing
     * the report bytes), {@link ParseException} (parsing a formatted date), and {@link MessagingException}
     * (constructing the delivery message) &mdash; all collapsed into a single {@code catch} and rethrown
     * as the unchecked {@link ReportExportException}, so this method declares no checked exception.</p>
     */
    @Override
    @Transactional("hibernateTransactionManager")
    public String exportBookOfBusinessReport(Long agentId) {
        requireAgent(agentId);
        List<Policy> policies = reportDao.findAgentBookOfBusiness(agentId);
        try {
            // (1) Render the report rows to an in-memory byte stream (java.io.IOException).
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            Writer writer = new OutputStreamWriter(buffer, StandardCharsets.UTF_8);
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            writer.write("Book of Business - Agent " + agentId + System.lineSeparator());
            for (Policy policy : policies) {
                // (2) Round-trip the term end date through a formatter (java.text.ParseException).
                String termEnd = policy.getTermEndDate() == null ? "" : policy.getTermEndDate().toString();
                if (!termEnd.isEmpty()) {
                    Date parsed = dateFormat.parse(termEnd);
                    termEnd = dateFormat.format(parsed);
                }
                writer.write(policy.getPolicyNumber() + "," + policy.getPolicyStatus() + ","
                        + policy.getTotalPremium() + "," + termEnd + System.lineSeparator());
            }
            writer.flush();
            byte[] rendered = buffer.toByteArray();

            // (3) Build the delivery message carrying the rendered report (javax.mail.MessagingException).
            Session mailSession = Session.getInstance(new Properties());
            MimeMessage message = new MimeMessage(mailSession);
            message.setFrom(new InternetAddress("reports@insurancepolicyadmin.com"));
            message.setSubject("Book of Business - Agent " + agentId);
            message.setText(new String(rendered, StandardCharsets.UTF_8));
            // The constructed MimeMessage is intentionally not dispatched; it exists to make the
            // MessagingException path real.
            message.getSubject();

            return new String(rendered, StandardCharsets.UTF_8);
        } catch (IOException | ParseException | MessagingException e) {
            throw new ReportExportException(
                    "Failed to export book-of-business report for agent " + agentId, e);
        }
    }

    private void requireAgent(Long agentId) {
        if (agentId == null || !agentDao.findById(agentId).isPresent()) {
            throw new AgentNotFoundException(agentId);
        }
    }

    private RenewalNoticeResponse toRenewalNoticeResponse(RenewalNotice notice) {
        RenewalNoticeResponse response = new RenewalNoticeResponse();
        response.setRenewalNoticeId(notice.getRenewalNoticeId());
        response.setPolicyId(notice.getPolicyId());
        response.setGeneratedAt(notice.getGeneratedAt());
        response.setTermEndDateSnapshot(notice.getTermEndDateSnapshot());
        return response;
    }

    private PolicySummaryResponse toPolicySummaryResponse(Policy policy) {
        PolicySummaryResponse response = new PolicySummaryResponse();
        response.setPolicyId(policy.getPolicyId());
        response.setPolicyNumber(policy.getPolicyNumber());
        response.setCustomerId(policy.getCustomerId());
        response.setPolicyStatus(policy.getPolicyStatus());
        response.setTermEndDate(policy.getTermEndDate());
        response.setTotalPremium(policy.getTotalPremium());
        return response;
    }

    private OverdueInstallmentResponse toOverdueInstallmentResponse(ReportDao.OverdueInstallment row,
                                                                    LocalDate today) {
        OverdueInstallmentResponse response = new OverdueInstallmentResponse();
        response.setInstallmentId(row.getInstallmentId());
        response.setDueDate(row.getDueDate());
        response.setAmountDue(row.getAmountDue());
        response.setAmountPaid(row.getAmountPaid());
        response.setPolicyId(row.getPolicyId());
        response.setCustomerId(row.getCustomerId());
        if (row.getDueDate() != null) {
            long days = ChronoUnit.DAYS.between(row.getDueDate(), today);
            response.setDaysOverdue(days < 0L ? 0 : (int) days);
        }
        return response;
    }
}
