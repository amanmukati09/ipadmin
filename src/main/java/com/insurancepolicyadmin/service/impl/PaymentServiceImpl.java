package com.insurancepolicyadmin.service.impl;
import com.insurancepolicyadmin.dao.CustomerDao;
import com.insurancepolicyadmin.dao.PaymentDao;
import com.insurancepolicyadmin.dao.PolicyDao;
import com.insurancepolicyadmin.dao.PremiumDao;
import com.insurancepolicyadmin.dto.request.PaymentRequest;
import com.insurancepolicyadmin.dto.response.PaymentAllocationResponse;
import com.insurancepolicyadmin.dto.response.PaymentResponse;
import com.insurancepolicyadmin.exception.OverpaymentException;
import com.insurancepolicyadmin.exception.PolicyNotFoundException;
import com.insurancepolicyadmin.exception.ValidationException;
import com.insurancepolicyadmin.model.entity.Customer;
import com.insurancepolicyadmin.model.plain.Installment;
import com.insurancepolicyadmin.model.plain.Payment;
import com.insurancepolicyadmin.model.plain.Policy;
import com.insurancepolicyadmin.service.PaymentService;
import com.insurancepolicyadmin.service.PolicyService;
import com.insurancepolicyadmin.util.CustomerSegmentRouter;
import com.insurancepolicyadmin.util.ValidationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
/**
 * Default {@link PaymentService} implementation (Requirement 11).
 *
 * <p>Payment persistence and the oldest-unpaid-first allocation are delegated to the
 * stored-procedure-backed {@link PaymentDao} (its {@code JdbcPaymentDao} implementation calls
 * {@code sp_payment_apply}/{@code sp_payment_history_cursor} via Spring JDBC). Installment data is
 * read through {@link PremiumDao}, Policy existence through {@link PolicyDao}, and the owning
 * Customer's segment code through {@link CustomerDao}. Field validation uses the static
 * {@link ValidationUtils} helper; the per-segment payment grace period comes from
 * {@link CustomerSegmentRouter}. The 30-day-overdue lapse transition is driven through
 * {@link PolicyService#transitionStatus} (Requirement 11.10).</p>
 *
 * <p>{@link #recordPayment(Long, PaymentRequest)} runs under the Spring-JDBC-backed
 * {@code DataSourceTransactionManager} (qualifier {@code jdbcTransactionManager}), matching the DAOs
 * that own the Payment writes. The overpayment rollback itself is performed inside
 * {@code sp_payment_apply}; this service only translates the returned status code to an
 * {@link OverpaymentException} (Requirement 11.7).</p>
 */
@Service
public class PaymentServiceImpl implements PaymentService {
    private static final Logger LOG = LoggerFactory.getLogger(PaymentServiceImpl.class);
    private static final BigDecimal MIN_AMOUNT = new BigDecimal("0.01");
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("999999999.99");
    private static final String STATUS_ACTIVE = "active";
    private static final String STATUS_LAPSED = "lapsed";
    private static final String INSTALLMENT_PAID = "paid";
    /** Installments more than this many days past due (adjusted by grace period) trigger a lapse. */
    private static final int LAPSE_OVERDUE_DAYS = 30;
    private final PaymentDao paymentDao;
    private final PremiumDao premiumDao;
    private final PolicyDao policyDao;
    private final CustomerDao customerDao;
    private final PolicyService policyService;
    // INTENTIONAL TENANT BRANCHING: CustomerSegmentRouter is a plain legacy utility
    // (Requirement 16.2), instantiated directly rather than injected as a Spring bean,
    // consistent with how it is written and with BeneficiaryServiceImpl's usage.
    private final CustomerSegmentRouter customerSegmentRouter = new CustomerSegmentRouter();
    @Autowired
    public PaymentServiceImpl(PaymentDao paymentDao, PremiumDao premiumDao, PolicyDao policyDao,
                              CustomerDao customerDao, PolicyService policyService) {
        this.paymentDao = paymentDao;
        this.premiumDao = premiumDao;
        this.policyDao = policyDao;
        this.customerDao = customerDao;
        this.policyService = policyService;
    }
    /** {@inheritDoc} */
    @Override
    @Transactional("chainedTransactionManager")
    public PaymentAllocationResponse recordPayment(Long policyId, PaymentRequest request) {
        if (request == null) {
            throw new ValidationException("Payment request body is required.");
        }
        BigDecimal amount = request.getAmount();
        LocalDate paymentDate = request.getPaymentDate();
        // Requirement 11.4: amount must be within [0.01, 999,999,999.99].
        if (amount == null || amount.compareTo(MIN_AMOUNT) < 0 || amount.compareTo(MAX_AMOUNT) > 0) {
            throw new ValidationException(
                    "Payment amount must be between 0.01 and 999999999.99; received " + amount + ".");
        }
        // Requirement 11.5: payment date must not be after the current date.
        if (!ValidationUtils.isNotFutureDate(paymentDate)) {
            throw new ValidationException(
                    "Payment date must be provided and must not be after the current date; received "
                            + paymentDate + ".");
        }
        // Requirement 11.6: the Policy must exist.
        Policy policy = requirePolicy(policyId);
        // Requirement 11.1/11.2/11.3/11.7: delegate the oldest-unpaid-first allocation to the stored
        // procedure. Snapshot the unpaid Installments beforehand so the response can report the
        // per-Installment allocation breakdown produced by the same algorithm the procedure applies.
        List<Installment> unpaidOldestFirst = unpaidInstallments(policyId);
        AllocationResult allocation = allocatePayment(amount, unpaidOldestFirst);
        PaymentDao.PaymentResult result = paymentDao.applyPayment(policyId, amount, paymentDate);
        // Requirement 11.7: status code 1 (overpayment, including a zero remaining balance). The
        // stored procedure has already rolled back, so no state change has occurred here.
        if (result.getStatusCode() == PaymentDao.PaymentResult.STATUS_OVERPAYMENT) {
            throw new OverpaymentException(policyId, amount, totalRemaining(unpaidOldestFirst));
        }
        if (result.getStatusCode() != PaymentDao.PaymentResult.STATUS_SUCCESS) {
            throw new ValidationException(
                    "Payment could not be applied to policy " + policyId + " (status code "
                            + result.getStatusCode() + ").");
        }
        LOG.info("Recorded payment {} of {} against policy {}", result.getPaymentId(), amount, policyId);
        PaymentAllocationResponse response = toAllocationResponse(policyId, result.getPaymentId(),
                amount, paymentDate, allocation);
        // Requirement 11.10: after a successful payment, lapse an active Policy whose Premium_Schedule
        // still holds an unpaid Installment more than 30 (grace-adjusted) days past due.
        evaluateLapse(policy);
        return response;
    }
    /** {@inheritDoc} */
    @Override
    @Transactional("jdbcTransactionManager")
    public List<PaymentResponse> getPaymentHistory(Long policyId) {
        requirePolicy(policyId);
        List<Payment> payments = paymentDao.findHistoryByPolicyId(policyId);
        List<PaymentResponse> responses = new ArrayList<PaymentResponse>();
        if (payments != null) {
            for (Payment payment : payments) {
                responses.add(toPaymentResponse(payment));
            }
        }
        // Requirement 11.8: an existing Policy with no Payments yields an empty list, not an error.
        return responses;
    }
    /** {@inheritDoc} */
    @Override
    public AllocationResult allocatePayment(BigDecimal amount, List<Installment> unpaidOldestFirst) {
        List<InstallmentAllocation> allocations = new ArrayList<InstallmentAllocation>();
        if (amount == null || amount.signum() <= 0) {
            // A non-positive amount cannot be allocated; the caller validates this separately
            // (Requirement 11.4). Treat it as a non-overpayment with no allocations.
            return new AllocationResult(allocations, false);
        }
        BigDecimal totalRemaining = totalRemaining(unpaidOldestFirst);
        // Requirement 11.7: an amount exceeding the total remaining unpaid balance (including a zero
        // total) is an overpayment with no allocation and no state change.
        if (amount.compareTo(totalRemaining) > 0) {
            return new AllocationResult(allocations, true);
        }
        BigDecimal available = amount;
        if (unpaidOldestFirst != null) {
            for (Installment installment : unpaidOldestFirst) {
                if (available.signum() <= 0) {
                    break;
                }
                BigDecimal remaining = remainingDue(installment);
                if (remaining.signum() <= 0) {
                    continue;
                }
                BigDecimal applied = available.min(remaining);
                allocations.add(new InstallmentAllocation(
                        installment == null ? null : installment.getInstallmentId(), applied));
                available = available.subtract(applied);
            }
        }
        return new AllocationResult(allocations, false);
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
    /** The Policy's unpaid Installments (remaining balance &gt; 0), ordered by due date ascending. */
    private List<Installment> unpaidInstallments(Long policyId) {
        List<Installment> all = premiumDao.findScheduleByPolicyId(policyId);
        List<Installment> unpaid = new ArrayList<Installment>();
        if (all != null) {
            for (Installment installment : all) {
                if (remainingDue(installment).signum() > 0) {
                    unpaid.add(installment);
                }
            }
        }
        return unpaid;
    }
    /**
     * Evaluates Requirement 11.10 after a successful Payment: when the Policy is {@code active} and
     * still has an unpaid Installment whose due date is more than the (segment-adjusted) grace period
     * plus 30 days in the past, the Policy is transitioned to {@code lapsed} via
     * {@link PolicyService#transitionStatus}. The trigger is best-effort: a rejected transition (for
     * example, a concurrent status change) is logged and swallowed so it does not undo the recorded
     * Payment.
     */
    private void evaluateLapse(Policy policy) {
        if (policy == null || !STATUS_ACTIVE.equals(policy.getPolicyStatus())) {
            return;
        }
        int graceDays = customerSegmentRouter.getPaymentGracePeriodDays(resolveSegmentCode(policy));
        LocalDate cutoff = LocalDate.now().minusDays((long) LAPSE_OVERDUE_DAYS + graceDays);
        boolean overdue = false;
        List<Installment> installments = premiumDao.findScheduleByPolicyId(policy.getPolicyId());
        if (installments != null) {
            for (Installment installment : installments) {
                LocalDate dueDate = installment.getDueDate();
                if (!INSTALLMENT_PAID.equals(installment.getInstallmentStatus())
                        && remainingDue(installment).signum() > 0
                        && dueDate != null && dueDate.isBefore(cutoff)) {
                    overdue = true;
                    break;
                }
            }
        }
        if (!overdue) {
            return;
        }
        try {
            policyService.transitionStatus(policy.getPolicyId(), STATUS_LAPSED);
            LOG.info("Lapsed policy {} after detecting an installment overdue by more than {} days",
                    policy.getPolicyId(), LAPSE_OVERDUE_DAYS);
        } catch (RuntimeException ex) {
            LOG.warn("Lapse transition for policy {} was not applied: {}", policy.getPolicyId(),
                    ex.getMessage());
        }
    }
    private String resolveSegmentCode(Policy policy) {
        if (policy == null || policy.getCustomerId() == null) {
            return null;
        }
        Optional<Customer> customer = customerDao.findById(policy.getCustomerId());
        return customer.map(Customer::getSegmentCode).orElse(null);
    }
    private static BigDecimal remainingDue(Installment installment) {
        if (installment == null || installment.getAmountDue() == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal paid = installment.getAmountPaid() == null ? BigDecimal.ZERO : installment.getAmountPaid();
        BigDecimal remaining = installment.getAmountDue().subtract(paid);
        return remaining.signum() < 0 ? BigDecimal.ZERO : remaining;
    }
    private static BigDecimal totalRemaining(List<Installment> installments) {
        BigDecimal total = BigDecimal.ZERO;
        if (installments != null) {
            for (Installment installment : installments) {
                total = total.add(remainingDue(installment));
            }
        }
        return total;
    }
    private static PaymentAllocationResponse toAllocationResponse(Long policyId, Long paymentId,
                                                                  BigDecimal amount, LocalDate paymentDate,
                                                                  AllocationResult allocation) {
        PaymentAllocationResponse response = new PaymentAllocationResponse();
        response.setPaymentId(paymentId);
        response.setPolicyId(policyId);
        response.setPaymentAmount(amount);
        response.setPaymentDate(paymentDate);
        List<PaymentAllocationResponse.AllocationItem> items =
                new ArrayList<PaymentAllocationResponse.AllocationItem>();
        for (InstallmentAllocation allocated : allocation.getAllocations()) {
            PaymentAllocationResponse.AllocationItem item = new PaymentAllocationResponse.AllocationItem();
            item.setInstallmentId(allocated.getInstallmentId());
            item.setAmountApplied(allocated.getAmountApplied());
            items.add(item);
        }
        response.setAllocations(items);
        return response;
    }
    private static PaymentResponse toPaymentResponse(Payment payment) {
        PaymentResponse response = new PaymentResponse();
        response.setPaymentId(payment.getPaymentId());
        response.setPolicyId(payment.getPolicyId());
        response.setPaymentAmount(payment.getPaymentAmount());
        response.setPaymentDate(payment.getPaymentDate());
        response.setCreatedAt(payment.getCreatedAt());
        return response;
    }
}
