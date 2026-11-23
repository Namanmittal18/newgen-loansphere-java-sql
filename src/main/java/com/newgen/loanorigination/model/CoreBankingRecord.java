package com.newgen.loanorigination.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class CoreBankingRecord {
    private Long coreBankingId;
    private Long customerId;
    private Long applicationId;
    private String customerName;
    private String address;
    private LocalDate dateOfBirth;
    private String kycDetails;
    private BigDecimal approvedAmount;
    private BigDecimal disbursedAmount;
    private LocalDateTime disbursedDate;
    private LocalDateTime pushedAt;

    public CoreBankingRecord() {
    }

    public CoreBankingRecord(Long coreBankingId, Long customerId, Long applicationId, String customerName,
                             String address, LocalDate dateOfBirth, String kycDetails, BigDecimal approvedAmount,
                             BigDecimal disbursedAmount, LocalDateTime disbursedDate, LocalDateTime pushedAt) {
        this.coreBankingId = coreBankingId;
        this.customerId = customerId;
        this.applicationId = applicationId;
        this.customerName = customerName;
        this.address = address;
        this.dateOfBirth = dateOfBirth;
        this.kycDetails = kycDetails;
        this.approvedAmount = approvedAmount;
        this.disbursedAmount = disbursedAmount;
        this.disbursedDate = disbursedDate;
        this.pushedAt = pushedAt;
    }

    public Long getCoreBankingId() {
        return coreBankingId;
    }

    public void setCoreBankingId(Long coreBankingId) {
        this.coreBankingId = coreBankingId;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public Long getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(Long applicationId) {
        this.applicationId = applicationId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getKycDetails() {
        return kycDetails;
    }

    public void setKycDetails(String kycDetails) {
        this.kycDetails = kycDetails;
    }

    public BigDecimal getApprovedAmount() {
        return approvedAmount;
    }

    public void setApprovedAmount(BigDecimal approvedAmount) {
        this.approvedAmount = approvedAmount;
    }

    public BigDecimal getDisbursedAmount() {
        return disbursedAmount;
    }

    public void setDisbursedAmount(BigDecimal disbursedAmount) {
        this.disbursedAmount = disbursedAmount;
    }

    public LocalDateTime getDisbursedDate() {
        return disbursedDate;
    }

    public void setDisbursedDate(LocalDateTime disbursedDate) {
        this.disbursedDate = disbursedDate;
    }

    public LocalDateTime getPushedAt() {
        return pushedAt;
    }

    public void setPushedAt(LocalDateTime pushedAt) {
        this.pushedAt = pushedAt;
    }
}
