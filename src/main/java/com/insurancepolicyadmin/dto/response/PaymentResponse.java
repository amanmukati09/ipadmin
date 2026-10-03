package com.insurancepolicyadmin.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Response DTO for a Payment, mirroring {@code model.plain.Payment}'s fields.
 * Used for payment history listing (design.md REST API table, endpoint 10:
 * {@code GET /api/policies/{policyId}/payments}).
 */
public class PaymentResponse {
    private Long paymentId;
    private Long policyId;
    private BigDecimal paymentAmount;
    private LocalDate paymentDate;
    private LocalDateTime createdAt;

    public PaymentResponse() {
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
