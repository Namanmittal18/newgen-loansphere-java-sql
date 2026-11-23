package com.newgen.loanorigination.dto.reports;

import java.time.LocalDateTime;

public class CustomerSummaryReportDTO {
    private Long customerId;
    private String customerCode;
    private String customerName;
    private String mobileNumber;
    private String city;
    private String state;
    private LocalDateTime customerSince;

    public CustomerSummaryReportDTO() {
    }

    public CustomerSummaryReportDTO(Long customerId, String customerCode, String customerName,
                                    String mobileNumber, String city, String state, LocalDateTime customerSince) {
        this.customerId = customerId;
        this.customerCode = customerCode;
        this.customerName = customerName;
        this.mobileNumber = mobileNumber;
        this.city = city;
        this.state = state;
        this.customerSince = customerSince;
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

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public LocalDateTime getCustomerSince() {
        return customerSince;
    }

    public void setCustomerSince(LocalDateTime customerSince) {
        this.customerSince = customerSince;
    }
}
