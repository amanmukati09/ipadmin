package com.insurancepolicyadmin.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request DTO for {@code POST /api/policies/{policyId}/payments}.
 */
public class PaymentRequest {

    private BigDecimal amount;
    private LocalDate paymentDate;

    public PaymentRequest() {
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDate paymentDate) {
        this.paymentDate = paymentDate;
    }
}
