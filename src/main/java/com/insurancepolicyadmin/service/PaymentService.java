package com.insurancepolicyadmin.service;
import com.insurancepolicyadmin.dto.request.PaymentRequest;
import com.insurancepolicyadmin.dto.response.PaymentAllocationResponse;
import com.insurancepolicyadmin.dto.response.PaymentResponse;
import com.insurancepolicyadmin.model.plain.Installment;
import java.math.BigDecimal;
import java.util.List;
/**
 * Service-layer contract for Payment processing (Requirement 11).
 *
 * <p>Implemented by {@code PaymentServiceImpl}, which delegates Payment persistence and the
 * oldest-unpaid-first allocation to {@code JdbcPaymentDao} (stored procedures via Spring JDBC),
 * reads Installment data through {@code PremiumDao}, confirms Policy existence through
 * {@code PolicyDao}, validates request fields through {@code ValidationUtils}, and consults
 * {@code CustomerSegmentRouter} for the per-segment payment grace period. The 30-day-overdue lapse
 * transition (Requirement 11.10) is driven through {@link PolicyService#transitionStatus}.</p>
 *
 * <p>The pure allocation arithmetic is exposed as {@link #allocatePayment(BigDecimal, List)} so it
 * can be exercised directly by property tests (design.md Property 2, Task 18.2) without any database
 * access.</p>
 */
public interface PaymentService {
    /**
     * Records a Payment against a Policy's Premium_Schedule, applying it to the oldest unpaid
     * Installment first and any remainder to subsequently due Installments in due-date order
     * (Requirements 11.1, 11.2, 11.3).
     *
     * <p>Rejects a Payment amount outside {@code [0.01, 999,999,999.99]} or a future payment date
     * with a {@link com.insurancepolicyadmin.exception.ValidationException} (Requirements 11.4,
     * 11.5), a non-existent Policy with a {@link com.insurancepolicyadmin.exception.PolicyNotFoundException}
     * (Requirement 11.6), and an amount exceeding the total remaining unpaid balance (including a
     * zero remaining balance) with an {@link com.insurancepolicyadmin.exception.OverpaymentException}
     * and no state change (Requirement 11.7). After a successful Payment, when the Policy is
     * {@code active} and still has an Installment more than 30 days (grace-period-adjusted) past its
     * due date and unpaid, the Policy is transitioned to {@code lapsed} via
     * {@link PolicyService#transitionStatus} (Requirement 11.10).</p>
     *
     * @param policyId the Policy identifier the Payment is for
     * @param request  the Payment request (amount and payment date)
     * @return the created Payment with its per-Installment allocation breakdown
     */
    PaymentAllocationResponse recordPayment(Long policyId, PaymentRequest request);
    /**
     * Retrieves a Policy's Payment history, ordered by payment date descending (Requirements 11.8,
     * 11.9). Returns an empty list when the Policy has no Payments.
     *
     * @param policyId the Policy identifier
     * @return the Policy's Payment records, ordered by payment date descending
     */
    List<PaymentResponse> getPaymentHistory(Long policyId);
    /**
     * Pure, side-effect-free computation of how a Payment amount is allocated across a Policy's
     * unpaid Installments, mirroring {@code sp_payment_apply}'s oldest-unpaid-first algorithm exactly
     * so the Java and database layers agree (Requirements 11.1, 11.2, 11.3, 11.7).
     *
     * <p>The {@code unpaidOldestFirst} list is expected in due-date-ascending order; each
     * Installment's remaining unpaid balance is {@code amountDue - amountPaid}. The amount is applied
     * to each Installment in turn, taking the lesser of the Installment's remaining balance and the
     * payment amount still available, until the amount is exhausted. When the amount exceeds the
     * total remaining unpaid balance (including a zero total), the result is flagged as an
     * overpayment with no allocations (mirroring the stored procedure's rejection).</p>
     *
     * @param amount            the Payment amount to allocate (must be positive)
     * @param unpaidOldestFirst the Policy's unpaid Installments, ordered by due date ascending
     * @return the allocation outcome: per-Installment amounts applied and the overpayment flag
     */
    AllocationResult allocatePayment(BigDecimal amount, List<Installment> unpaidOldestFirst);
    /**
     * The outcome of a pure {@link PaymentService#allocatePayment(BigDecimal, List)} computation: an
     * ordered list of per-Installment allocations plus a flag indicating whether the amount exceeded
     * the total remaining unpaid balance.
     */
    class AllocationResult {
        private final List<InstallmentAllocation> allocations;
        private final boolean overpayment;
        public AllocationResult(List<InstallmentAllocation> allocations, boolean overpayment) {
            this.allocations = allocations;
            this.overpayment = overpayment;
        }
        public List<InstallmentAllocation> getAllocations() {
            return allocations;
        }
        public boolean isOverpayment() {
            return overpayment;
        }
    }
    /**
     * A single Installment's share of an allocated Payment: the target Installment identifier (may be
     * {@code null} for a transient/unsaved Installment) and the amount applied to it.
     */
    class InstallmentAllocation {
        private final Long installmentId;
        private final BigDecimal amountApplied;
        public InstallmentAllocation(Long installmentId, BigDecimal amountApplied) {
            this.installmentId = installmentId;
            this.amountApplied = amountApplied;
        }
        public Long getInstallmentId() {
            return installmentId;
        }
        public BigDecimal getAmountApplied() {
            return amountApplied;
        }
    }
}
