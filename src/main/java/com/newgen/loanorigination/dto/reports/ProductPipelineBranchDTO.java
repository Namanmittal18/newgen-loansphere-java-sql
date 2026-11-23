package com.newgen.loanorigination.dto.reports;

import java.math.BigDecimal;

public class ProductPipelineBranchDTO {
    private String branchCode;
    private String branchName;
    private String productType;
    private long applicationCount;
    private BigDecimal totalRequestedAmount;

    public ProductPipelineBranchDTO() {
    }

    public ProductPipelineBranchDTO(String branchCode, String branchName, String productType,
                                    long applicationCount, BigDecimal totalRequestedAmount) {
        this.branchCode = branchCode;
        this.branchName = branchName;
        this.productType = productType;
        this.applicationCount = applicationCount;
        this.totalRequestedAmount = totalRequestedAmount;
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

    public String getProductType() {
        return productType;
    }

    public void setProductType(String productType) {
        this.productType = productType;
    }

    public long getApplicationCount() {
        return applicationCount;
    }

    public void setApplicationCount(long applicationCount) {
        this.applicationCount = applicationCount;
    }

    public BigDecimal getTotalRequestedAmount() {
        return totalRequestedAmount;
    }

    public void setTotalRequestedAmount(BigDecimal totalRequestedAmount) {
        this.totalRequestedAmount = totalRequestedAmount;
    }
}
