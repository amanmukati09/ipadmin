package com.insurancepolicyadmin.dao;

import com.insurancepolicyadmin.model.plain.Payment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Technology-neutral DAO contract for {@link Payment} persistence and allocation operations.
 *
 * <p>Implemented by {@code JdbcPaymentDao} using {@code SimpleJdbcCall}/
 * {@code CallableStatement} through {@code JdbcTemplate} exclusively, invoking stored
 * procedures. This interface SHALL NOT reference Hibernate, JPA, {@code JdbcTemplate},
 * or any other persistence-technology-specific type in its method signatures.</p>
 */
public interface PaymentDao {

    /**
     * Applies a Payment to a Policy's oldest unpaid Installment first, applying any
     * remainder to subsequently due Installments in due-date order.
     *
     * @param policyId    the Policy identifier
     * @param amount      the payment amount (0.01 to 999,999,999.99)
     * @param paymentDate the payment date (not after the current date)
     * @return the result of applying the Payment, including the generated Payment
     *         identifier and the resulting status code
     */
    PaymentResult applyPayment(Long policyId, BigDecimal amount, LocalDate paymentDate);

    /**
     * Retrieves the Payment history of a Policy, ordered by payment date descending.
     *
     * @param policyId the Policy identifier
     * @return the matching Payment records, or an empty list if none exist
     */
    List<Payment> findHistoryByPolicyId(Long policyId);

    /**
     * The outcome of an {@link #applyPayment(Long, BigDecimal, LocalDate)} invocation.
     *
     * <p>A plain, technology-neutral result holder carrying the generated Payment
     * identifier (when the payment succeeded) and a status code indicating the outcome.</p>
     */
    class PaymentResult {

        /** Status code indicating the Payment was applied successfully. */
        public static final int STATUS_SUCCESS = 0;

        /** Status code indicating the Payment amount exceeded the remaining unpaid balance. */
        public static final int STATUS_OVERPAYMENT = 1;

        private final Long paymentId;
        private final int statusCode;

        public PaymentResult(Long paymentId, int statusCode) {
            this.paymentId = paymentId;
            this.statusCode = statusCode;
        }

        public Long getPaymentId() {
            return paymentId;
        }

        public int getStatusCode() {
            return statusCode;
        }
    }
}
