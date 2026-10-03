package com.insurancepolicyadmin.controller;

import com.insurancepolicyadmin.dao.PremiumDao;
import com.insurancepolicyadmin.dto.request.PaymentRequest;
import com.insurancepolicyadmin.dto.response.PaymentAllocationResponse;
import com.insurancepolicyadmin.dto.response.PaymentResponse;
import com.insurancepolicyadmin.exception.OverpaymentException;
import com.insurancepolicyadmin.model.plain.Installment;
import com.insurancepolicyadmin.service.PaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

/**
 * REST controller for Payment processing (design.md REST API table, endpoints 9-10):
 * recording a Payment against a Policy's Premium_Schedule and retrieving a Policy's
 * Payment history.
 *
 * <p>Endpoints:</p>
 * <ul>
 *   <li>{@code POST /api/policies/{policyId}/payments} - record a Payment (endpoint 9)</li>
 *   <li>{@code GET  /api/policies/{policyId}/payments} - Payment history (endpoint 10)</li>
 * </ul>
 *
 * <p><b>Intentional layering violation (Requirement 16.3):</b> this controller deliberately
 * embeds the overpayment business check directly in {@link #recordPayment} - computing the
 * Policy's remaining unpaid balance via {@link PremiumDao} and rejecting over-payments before
 * delegating to {@link PaymentService}. The same rule is also (correctly) enforced inside
 * {@code PaymentServiceImpl}; the duplicated/misplaced check here is the intended code smell and
 * must NOT be refactored into the service. This is the ONLY business logic embedded in this
 * controller - everything else delegates cleanly to {@link PaymentService}.</p>
 */
@RestController
@RequestMapping("/policies/{policyId}/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final PremiumDao premiumDao;

    public PaymentController(PaymentService paymentService, PremiumDao premiumDao) {
        this.paymentService = paymentService;
        this.premiumDao = premiumDao;
    }

    /**
     * Records a Payment against a Policy (design.md endpoint 9).
     *
     * @param policyId the Policy identifier
     * @param request  the Payment request (amount and payment date)
     * @return 201 with the created Payment's per-Installment allocation breakdown
     */
    @PostMapping
    public ResponseEntity<PaymentAllocationResponse> recordPayment(@PathVariable Long policyId,
                                                                   @RequestBody PaymentRequest request) {
        BigDecimal amount = request.getAmount();

        // Compute the Policy's total remaining unpaid balance by summing the outstanding
        // (amountDue - amountPaid) of every Installment on the schedule.
        BigDecimal remainingBalance = BigDecimal.ZERO;
        List<Installment> installments = premiumDao.findScheduleByPolicyId(policyId);
        for (Installment installment : installments) {
            BigDecimal due = installment.getAmountDue() == null ? BigDecimal.ZERO : installment.getAmountDue();
            BigDecimal paid = installment.getAmountPaid() == null ? BigDecimal.ZERO : installment.getAmountPaid();
            BigDecimal outstanding = due.subtract(paid);
            if (outstanding.signum() > 0) {
                remainingBalance = remainingBalance.add(outstanding);
            }
        }

        // INTENTIONAL LAYERING VIOLATION
        if (amount != null && amount.compareTo(remainingBalance) > 0) {
            throw new OverpaymentException(policyId, amount, remainingBalance);
        }

        PaymentAllocationResponse response = paymentService.recordPayment(policyId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retrieves a Policy's Payment history, ordered by payment date descending
     * (design.md endpoint 10).
     *
     * @param policyId the Policy identifier
     * @return 200 with the Policy's Payment records
     */
    @GetMapping
    public ResponseEntity<List<PaymentResponse>> getPaymentHistory(@PathVariable Long policyId) {
        return ResponseEntity.ok(paymentService.getPaymentHistory(policyId));
    }
}
