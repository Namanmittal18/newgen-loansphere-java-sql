package com.newgen.loanorigination.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Disbursement {
    private Long disbursementId;
    private Long applicationId;
    private String disbursementReference;
    private BigDecimal amount;
    private LocalDateTime disbursedAt;

    public Disbursement() {
    }

    public Disbursement(Long disbursementId, Long applicationId, String disbursementReference,
                        BigDecimal amount, LocalDateTime disbursedAt) {
        this.disbursementId = disbursementId;
        this.applicationId = applicationId;
        this.disbursementReference = disbursementReference;
        this.amount = amount;
        this.disbursedAt = disbursedAt;
    }

    public Long getDisbursementId() {
        return disbursementId;
    }

    public void setDisbursementId(Long disbursementId) {
        this.disbursementId = disbursementId;
    }

    public Long getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(Long applicationId) {
        this.applicationId = applicationId;
    }

    public String getDisbursementReference() {
        return disbursementReference;
    }

    public void setDisbursementReference(String disbursementReference) {
        this.disbursementReference = disbursementReference;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDateTime getDisbursedAt() {
        return disbursedAt;
    }

    public void setDisbursedAt(LocalDateTime disbursedAt) {
        this.disbursedAt = disbursedAt;
    }
}
