package com.insurancepolicyadmin.model.plain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Plain (non-JPA) model for the {@code payment} table.
 * Persisted exclusively through {@code JdbcPaymentDao} via stored procedures.
 * Contains no JPA/Hibernate annotations per Requirement 2.8.
 */
public class Payment {

    private Long paymentId;
    private Long policyId;
    private BigDecimal paymentAmount;
    private LocalDate paymentDate;
    private LocalDateTime createdAt;

    public Payment() {
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
