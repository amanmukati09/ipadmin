package com.insurancepolicyadmin.service;

import com.insurancepolicyadmin.model.plain.Installment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Service-layer contract for Premium_Schedule and Installment calculation (Requirement 10).
 *
 * <p>Implemented by {@code PremiumServiceImpl}, which delegates persistence to
 * {@code JdbcPremiumDao} (stored procedures via Spring JDBC) and keeps the pure
 * installment-calculation arithmetic in {@link #calculateInstallments(BigDecimal, String, LocalDate)}
 * so it can be exercised directly by property tests (design.md Property 1, Task 16.2).</p>
 *
 * <p>Consumed by {@code PolicyService} during binding (it calls
 * {@link #generateSchedule(Long, BigDecimal, String, LocalDate)} once a Policy transitions out of
 * {@code quoted}) and by {@code EndorsementService} when a mid-term change requires the Premium_Schedule
 * to be regenerated.</p>
 */
public interface PremiumService {

    /**
     * Generates a Policy's Premium_Schedule and Installments.
     *
     * <p>Rejects the request (throwing {@code ScheduleGenerationException}) when the Policy's status is
     * neither {@code quoted} nor {@code bound} (Requirement 10.4), or when the Policy already has a
     * Premium_Schedule with at least one Installment (Requirement 10.5). When the referenced Policy does
     * not exist, a {@code PolicyNotFoundException} is thrown (Requirement 10.7). On success, the
     * generated Installments (ordered by due date ascending) are returned.</p>
     *
     * @param policyId         the Policy identifier
     * @param totalPremium     the Policy's total premium amount
     * @param billingFrequency the billing frequency ({@code monthly}, {@code quarterly},
     *                         {@code semi_annual}, {@code annual})
     * @param termStartDate    the Policy term start date, used as the first Installment's due date
     * @return the generated Installment records, ordered by due date ascending
     */
    List<Installment> generateSchedule(Long policyId, BigDecimal totalPremium, String billingFrequency,
                                       LocalDate termStartDate);

    /**
     * Retrieves a Policy's Premium_Schedule Installments, ordered by due date ascending
     * (Requirement 10.6).
     *
     * <p>Throws {@code PolicyNotFoundException} when the referenced Policy does not exist
     * (Requirement 10.7).</p>
     *
     * @param policyId the Policy identifier
     * @return the Policy's Installment records, ordered by due date ascending
     */
    List<Installment> getSchedule(Long policyId);

    /**
     * Pure, side-effect-free computation of the Installment lines for a Premium_Schedule, mirroring the
     * {@code sp_premium_schedule_generate_installments} stored procedure's algorithm exactly so the Java
     * and database layers agree (Requirements 10.1, 10.2, 10.3).
     *
     * <p>Each returned {@link Installment} carries only its {@code dueDate} and {@code amountDue}; the
     * remaining fields are left unset because this method does not touch the database. The installment
     * count follows the billing frequency (12/4/2/1), due dates follow the frequency's month offset from
     * {@code termStartDate}, each non-final amount is the floor-rounded (to 2 decimals) per-installment
     * share, and the final installment absorbs the rounding remainder so the amounts sum to
     * {@code totalPremium} exactly.</p>
     *
     * @param totalPremium     the Policy's total premium amount (positive, up to 2 decimal places)
     * @param billingFrequency the billing frequency ({@code monthly}, {@code quarterly},
     *                         {@code semi_annual}, {@code annual})
     * @param termStartDate    the Policy term start date (first Installment's due date)
     * @return the computed Installment lines, ordered by due date ascending
     */
    List<Installment> calculateInstallments(BigDecimal totalPremium, String billingFrequency,
                                            LocalDate termStartDate);
}
