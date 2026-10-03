package com.insurancepolicyadmin.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Response DTO for an Installment, mirroring {@code model.plain.Installment}'s fields.
 */
public class InstallmentResponse {
    private Long installmentId;
    private Long premiumScheduleId;
    private LocalDate dueDate;
    private BigDecimal amountDue;
    private BigDecimal amountPaid;
    private String installmentStatus;

    public InstallmentResponse() {
    }

    public Long getInstallmentId() {
        return installmentId;
    }

    public void setInstallmentId(Long installmentId) {
        this.installmentId = installmentId;
    }

    public Long getPremiumScheduleId() {
        return premiumScheduleId;
    }

    public void setPremiumScheduleId(Long premiumScheduleId) {
        this.premiumScheduleId = premiumScheduleId;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public BigDecimal getAmountDue() {
        return amountDue;
    }

    public void setAmountDue(BigDecimal amountDue) {
        this.amountDue = amountDue;
    }

    public BigDecimal getAmountPaid() {
        return amountPaid;
    }

    public void setAmountPaid(BigDecimal amountPaid) {
        this.amountPaid = amountPaid;
    }

    public String getInstallmentStatus() {
        return installmentStatus;
    }

    public void setInstallmentStatus(String installmentStatus) {
        this.installmentStatus = installmentStatus;
    }
}
