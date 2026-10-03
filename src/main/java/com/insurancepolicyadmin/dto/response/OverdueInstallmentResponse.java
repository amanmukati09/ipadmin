package com.insurancepolicyadmin.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Response DTO for {@code GET /api/reports/overdue-payments} (design.md REST API table,
 * endpoint 14), backed by {@code sp_report_overdue_installments}. Each row is an overdue
 * Installment (mirroring {@code model.plain.Installment}'s fields) joined with the owning
 * Policy's identifying fields, since an overdue-payments report is meaningless without
 * knowing which Policy/Customer each overdue Installment belongs to.
 */
public class OverdueInstallmentResponse {
    private Long installmentId;
    private Long premiumScheduleId;
    private LocalDate dueDate;
    private BigDecimal amountDue;
    private BigDecimal amountPaid;
    private String installmentStatus;
    private Long policyId;
    private String policyNumber;
    private Long customerId;
    private int daysOverdue;

    public OverdueInstallmentResponse() {
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

    public Long getPolicyId() {
        return policyId;
    }

    public void setPolicyId(Long policyId) {
        this.policyId = policyId;
    }

    public String getPolicyNumber() {
        return policyNumber;
    }

    public void setPolicyNumber(String policyNumber) {
        this.policyNumber = policyNumber;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public int getDaysOverdue() {
        return daysOverdue;
    }

    public void setDaysOverdue(int daysOverdue) {
        this.daysOverdue = daysOverdue;
    }
}
