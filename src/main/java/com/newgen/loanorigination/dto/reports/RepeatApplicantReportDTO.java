package com.newgen.loanorigination.dto.reports;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class RepeatApplicantReportDTO {
    private Long customerId;
    private String customerCode;
    private String customerName;
    private String mobileNumber;
    private long totalApplications;
    private BigDecimal totalRequestedAmount;
    private LocalDateTime firstApplicationDate;
    private LocalDateTime latestApplicationDate;

    public RepeatApplicantReportDTO() {
    }

    public RepeatApplicantReportDTO(Long customerId, String customerCode, String customerName,
                                    String mobileNumber, long totalApplications, BigDecimal totalRequestedAmount,
                                    LocalDateTime firstApplicationDate, LocalDateTime latestApplicationDate) {
        this.customerId = customerId;
        this.customerCode = customerCode;
        this.customerName = customerName;
        this.mobileNumber = mobileNumber;
        this.totalApplications = totalApplications;
        this.totalRequestedAmount = totalRequestedAmount;
        this.firstApplicationDate = firstApplicationDate;
        this.latestApplicationDate = latestApplicationDate;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getCustomerCode() {
        return customerCode;
    }

    public void setCustomerCode(String customerCode) {
        this.customerCode = customerCode;
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

    public long getTotalApplications() {
        return totalApplications;
    }

    public void setTotalApplications(long totalApplications) {
        this.totalApplications = totalApplications;
    }

    public BigDecimal getTotalRequestedAmount() {
        return totalRequestedAmount;
    }

    public void setTotalRequestedAmount(BigDecimal totalRequestedAmount) {
        this.totalRequestedAmount = totalRequestedAmount;
    }

    public LocalDateTime getFirstApplicationDate() {
        return firstApplicationDate;
    }

    public void setFirstApplicationDate(LocalDateTime firstApplicationDate) {
        this.firstApplicationDate = firstApplicationDate;
    }

    public LocalDateTime getLatestApplicationDate() {
        return latestApplicationDate;
    }

    public void setLatestApplicationDate(LocalDateTime latestApplicationDate) {
        this.latestApplicationDate = latestApplicationDate;
    }
}
