package com.insurancepolicyadmin.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Response DTO returned by {@code POST /api/policies/{policyId}/payments} (design.md REST
 * API table, endpoint 9). Wraps the created {@code Payment}'s own fields together with the
 * list of individual {@code payment_allocation} rows produced by the oldest-unpaid-first
 * allocation algorithm, since a single Payment can be split across multiple Installments.
 */
public class PaymentAllocationResponse {
    private Long paymentId;
    private Long policyId;
    private BigDecimal paymentAmount;
    private LocalDate paymentDate;
    private List<AllocationItem> allocations;

    public PaymentAllocationResponse() {
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(Long paymentId) {
        this.paymentId = paymentId;
    }

    public Long getPolicyId() {
        return policyId;
    }

    public void setPolicyId(Long policyId) {
        this.policyId = policyId;
    }

    public BigDecimal getPaymentAmount() {
        return paymentAmount;
    }

    public void setPaymentAmount(BigDecimal paymentAmount) {
        this.paymentAmount = paymentAmount;
    }

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDate paymentDate) {
        this.paymentDate = paymentDate;
    }

    public List<AllocationItem> getAllocations() {
        return allocations;
    }

    public void setAllocations(List<AllocationItem> allocations) {
        this.allocations = allocations;
    }

    /**
     * A single {@code payment_allocation} row (one Installment's share of the Payment),
     * mirroring {@code model.plain.PaymentAllocation}'s fields.
     */
    public static class AllocationItem {
        private Long paymentAllocationId;
        private Long installmentId;
        private BigDecimal amountApplied;

        public AllocationItem() {
        }

        public Long getPaymentAllocationId() {
            return paymentAllocationId;
        }

        public void setPaymentAllocationId(Long paymentAllocationId) {
            this.paymentAllocationId = paymentAllocationId;
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
}
