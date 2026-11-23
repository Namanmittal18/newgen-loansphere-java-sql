package com.newgen.loanorigination.dto.reports;

import java.math.BigDecimal;

public class DisbursementSummaryDTO {
    private String disbursementPeriod;
    private String branchCode;
    private String branchName;
    private long disbursementCount;
    private BigDecimal totalDisbursedAmount;

    public DisbursementSummaryDTO() {
    }

    public DisbursementSummaryDTO(String disbursementPeriod, String branchCode, String branchName,
                                  long disbursementCount, BigDecimal totalDisbursedAmount) {
        this.disbursementPeriod = disbursementPeriod;
        this.branchCode = branchCode;
        this.branchName = branchName;
        this.disbursementCount = disbursementCount;
        this.totalDisbursedAmount = totalDisbursedAmount;
    }

    public String getDisbursementPeriod() {
        return disbursementPeriod;
    }

    public void setDisbursementPeriod(String disbursementPeriod) {
        this.disbursementPeriod = disbursementPeriod;
    }

    public String getBranchCode() {
        return branchCode;
    }

    public void setBranchCode(String branchCode) {
        this.branchCode = branchCode;
    }

    public String getBranchName() {
        return branchName;
    }

    public void setBranchName(String branchName) {
        this.branchName = branchName;
    }

    public long getDisbursementCount() {
        return disbursementCount;
    }

    public void setDisbursementCount(long disbursementCount) {
        this.disbursementCount = disbursementCount;
    }

    public BigDecimal getTotalDisbursedAmount() {
        return totalDisbursedAmount;
    }

    public void setTotalDisbursedAmount(BigDecimal totalDisbursedAmount) {
        this.totalDisbursedAmount = totalDisbursedAmount;
    }
}
