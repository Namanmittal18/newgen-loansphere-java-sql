package com.newgen.loanorigination.dto.reports;

import java.math.BigDecimal;

public class ProductPipelineStatusDTO {
    private String productType;
    private String status;
    private long applicationCount;
    private BigDecimal totalRequestedAmount;

    public ProductPipelineStatusDTO() {
    }

    public ProductPipelineStatusDTO(String productType, String status, long applicationCount, BigDecimal totalRequestedAmount) {
        this.productType = productType;
        this.status = status;
        this.applicationCount = applicationCount;
        this.totalRequestedAmount = totalRequestedAmount;
    }

    public String getProductType() {
        return productType;
    }

    public void setProductType(String productType) {
        this.productType = productType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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
