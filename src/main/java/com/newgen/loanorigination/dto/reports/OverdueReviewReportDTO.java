package com.newgen.loanorigination.dto.reports;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class OverdueReviewReportDTO {
    private Long applicationId;
    private String applicationNumber;
    private String customerName;
    private String mobileNumber;
    private String productType;
    private BigDecimal requestedAmount;
    private LocalDateTime submittedAt;
    private int daysWaiting;

    public OverdueReviewReportDTO() {
    }

    public OverdueReviewReportDTO(Long applicationId, String applicationNumber, String customerName,
                                  String mobileNumber, String productType, BigDecimal requestedAmount,
                                  LocalDateTime submittedAt, int daysWaiting) {
        this.applicationId = applicationId;
        this.applicationNumber = applicationNumber;
        this.customerName = customerName;
        this.mobileNumber = mobileNumber;
        this.productType = productType;
        this.requestedAmount = requestedAmount;
        this.submittedAt = submittedAt;
        this.daysWaiting = daysWaiting;
    }

    public Long getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(Long applicationId) {
        this.applicationId = applicationId;
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

    public String getMobileNumber() {
        return mobileNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
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

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public int getDaysWaiting() {
        return daysWaiting;
    }

    public void setDaysWaiting(int daysWaiting) {
        this.daysWaiting = daysWaiting;
    }
}
