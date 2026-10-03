package com.insurancepolicyadmin.service.impl;

import com.insurancepolicyadmin.dao.PolicyDao;
import com.insurancepolicyadmin.dao.PremiumDao;
import com.insurancepolicyadmin.exception.PolicyNotFoundException;
import com.insurancepolicyadmin.exception.ScheduleGenerationException;
import com.insurancepolicyadmin.model.plain.Installment;
import com.insurancepolicyadmin.model.plain.Policy;
import com.insurancepolicyadmin.service.PremiumService;
import com.insurancepolicyadmin.util.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Default {@link PremiumService} implementation.
 *
 * <p>Business rules (status/duplicate-schedule validation, not-found handling) live here; the actual
 * Premium_Schedule/Installment persistence is delegated to {@link PremiumDao} (its
 * {@code JdbcPremiumDao} implementation invokes MySQL stored procedures via Spring JDBC). Policy state
 * is read through {@link PolicyDao}. Mutating methods run under the Spring-JDBC-backed
 * {@code DataSourceTransactionManager} (qualifier {@code jdbcTransactionManager}) so schedule generation
 * commits atomically with the DAO's own stored-procedure transaction.</p>
 *
 * <p>The pure arithmetic used to lay out the Installment amounts and due dates is isolated in
 * {@link #calculateInstallments(BigDecimal, String, LocalDate)}, which performs no I/O and is therefore
 * directly property-testable (design.md Property 1, Task 16.2). It reproduces the
 * {@code sp_premium_schedule_generate_installments} algorithm from design.md's worked examples exactly:
 * {@code $1,200.00 / 12 -> 12 x $100.00}; {@code $1,000.00 / 12 -> 11 x $83.33 + $83.37}.</p>
 */
@Service
public class PremiumServiceImpl implements PremiumService {

    private static final Logger LOG = LoggerFactory.getLogger(PremiumServiceImpl.class);

    private static final int MONEY_SCALE = 2;

    private final PremiumDao premiumDao;
    private final PolicyDao policyDao;

    @Autowired
    public PremiumServiceImpl(PremiumDao premiumDao, PolicyDao policyDao) {
        this.premiumDao = premiumDao;
        this.policyDao = policyDao;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Validates the Policy exists and is in a bind-eligible state ({@code quoted} or {@code bound},
     * Requirement 10.4) and does not already have a Premium_Schedule with at least one Installment
     * (Requirement 10.5) before delegating generation to {@link PremiumDao#generateInstallments}. The
     * freshly generated Installments are then read back (ordered by due date ascending) and returned.</p>
     */
    @Override
    @Transactional("jdbcTransactionManager")
    public List<Installment> generateSchedule(Long policyId, BigDecimal totalPremium,
                                              String billingFrequency, LocalDate termStartDate) {
        Policy policy = requirePolicy(policyId);

        String status = policy.getPolicyStatus();
        if (!"quoted".equals(status) && !"bound".equals(status)) {
            throw new ScheduleGenerationException(
                    "Cannot generate a Premium_Schedule for policy " + policyId
                            + " in status '" + status + "'; the policy must be quoted or bound.");
        }

        List<Installment> existing = premiumDao.findScheduleByPolicyId(policyId);
        if (existing != null && !existing.isEmpty()) {
            throw new ScheduleGenerationException(
                    "A Premium_Schedule already exists for policy " + policyId
                            + "; it has " + existing.size() + " installment(s).");
        }

        LOG.info("Generating Premium_Schedule for policy {} ({} premium {}, start {})",
                policyId, billingFrequency, totalPremium, termStartDate);
        premiumDao.generateInstallments(policyId, totalPremium, billingFrequency, termStartDate);
        return premiumDao.findScheduleByPolicyId(policyId);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Delegates to {@link PremiumDao#findScheduleByPolicyId(Long)} after confirming the Policy exists,
     * so an unknown Policy identifier yields a {@code PolicyNotFoundException} (Requirement 10.7) rather
     * than an empty list indistinguishable from a Policy that genuinely has no Installments.</p>
     */
    @Override
    @Transactional("jdbcTransactionManager")
    public List<Installment> getSchedule(Long policyId) {
        requirePolicy(policyId);
        return premiumDao.findScheduleByPolicyId(policyId);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Algorithm (mirrors {@code sp_premium_schedule_generate_installments}):</p>
     * <ol>
     *   <li>{@code installmentCount} = 12/4/2/1 by billing frequency.</li>
     *   <li>{@code monthOffset} = 1/3/6/12 by billing frequency.</li>
     *   <li>{@code baseAmount = FLOOR(totalPremium / installmentCount, 2 decimals)}.</li>
     *   <li>Installments 1..(count-1): {@code baseAmount}, due at
     *       {@code termStartDate + (i * monthOffset) months}.</li>
     *   <li>Final installment: {@code totalPremium - baseAmount * (count - 1)} (absorbs the remainder).</li>
     * </ol>
     */
    @Override
    public List<Installment> calculateInstallments(BigDecimal totalPremium, String billingFrequency,
                                                   LocalDate termStartDate) {
        if (totalPremium == null) {
            throw new IllegalArgumentException("totalPremium must not be null");
        }
        if (termStartDate == null) {
            throw new IllegalArgumentException("termStartDate must not be null");
        }

        int installmentCount = DateUtils.installmentCountForFrequency(billingFrequency);
        int monthOffset = DateUtils.monthOffsetForFrequency(billingFrequency);

        // baseAmount = FLOOR(totalPremium / installmentCount) to 2 decimal places.
        BigDecimal baseAmount = totalPremium
                .divide(BigDecimal.valueOf(installmentCount), MONEY_SCALE, RoundingMode.FLOOR);

        List<Installment> lines = new ArrayList<Installment>(installmentCount);

        // Installments 1..(count-1) each carry baseAmount.
        for (int i = 0; i < installmentCount - 1; i++) {
            lines.add(newLine(termStartDate.plusMonths((long) i * monthOffset), baseAmount));
        }

        // Final installment absorbs the rounding remainder so the amounts sum to totalPremium exactly.
        BigDecimal precedingTotal = baseAmount.multiply(BigDecimal.valueOf(installmentCount - 1L));
        BigDecimal finalAmount = totalPremium.subtract(precedingTotal).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        int lastIndex = installmentCount - 1;
        lines.add(newLine(termStartDate.plusMonths((long) lastIndex * monthOffset), finalAmount));

        return lines;
    }

    // ------------------------------------------------------------------
    // Internal helpers
    // ------------------------------------------------------------------

    private Policy requirePolicy(Long policyId) {
        if (policyId == null) {
            throw new PolicyNotFoundException("Policy not found: null");
        }
        Optional<Policy> policy = policyDao.findById(policyId);
        if (!policy.isPresent()) {
            throw new PolicyNotFoundException(policyId);
        }
        return policy.get();
    }

    private static Installment newLine(LocalDate dueDate, BigDecimal amountDue) {
        Installment line = new Installment();
        line.setDueDate(dueDate);
        line.setAmountDue(amountDue);
        return line;
    }
}
