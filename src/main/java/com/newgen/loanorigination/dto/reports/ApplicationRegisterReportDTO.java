package com.newgen.loanorigination.dto.reports;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ApplicationRegisterReportDTO {
    private String applicationNumber;
    private String customerName;
    private String productType;
    private BigDecimal requestedAmount;
    private BigDecimal proposedEmi;
    private String status;
    private LocalDateTime submittedAt;

    public ApplicationRegisterReportDTO() {
    }

    public ApplicationRegisterReportDTO(String applicationNumber, String customerName, String productType,
                                        BigDecimal requestedAmount, BigDecimal proposedEmi, String status,
                                        LocalDateTime submittedAt) {
        this.applicationNumber = applicationNumber;
        this.customerName = customerName;
        this.productType = productType;
        this.requestedAmount = requestedAmount;
        this.proposedEmi = proposedEmi;
        this.status = status;
        this.submittedAt = submittedAt;
    }

    public String getApplicationNumber() {
        return applicationNumber;
    }

    public void setApplicationNumber(String applicationNumber) {
        this.applicationNumber = applicationNumber;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getProductType() {
        return productType;
    }

    public void setProductType(String productType) {
        this.productType = productType;
    }

    public BigDecimal getRequestedAmount() {
        return requestedAmount;
    }

    public void setRequestedAmount(BigDecimal requestedAmount) {
        this.requestedAmount = requestedAmount;
    }

    public BigDecimal getProposedEmi() {
        return proposedEmi;
    }

    public void setProposedEmi(BigDecimal proposedEmi) {
        this.proposedEmi = proposedEmi;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }
}
