package com.insurancepolicyadmin.model.plain;

import java.math.BigDecimal;

/**
 * Plain (non-JPA) model for the {@code payment_allocation} table.
 * Persisted exclusively through {@code JdbcPaymentDao} via stored procedures.
 * Contains no JPA/Hibernate annotations per Requirement 2.8.
 */
public class PaymentAllocation {

    private Long paymentAllocationId;
    private Long paymentId;
    private Long installmentId;
    private BigDecimal amountApplied;

    public PaymentAllocation() {
    }

    public Long getPaymentAllocationId() {
        return paymentAllocationId;
    }

    public void setPaymentAllocationId(Long paymentAllocationId) {
        this.paymentAllocationId = paymentAllocationId;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(Long paymentId) {
        this.paymentId = paymentId;
    }

    public Long getInstallmentId() {
        return installmentId;
    }

    public void setInstallmentId(Long installmentId) {
        this.installmentId = installmentId;
    }

    public BigDecimal getAmountApplied() {
        return amountApplied;
    }

    public void setAmountApplied(BigDecimal amountApplied) {
        this.amountApplied = amountApplied;
    }
}
