package com.newgen.loanorigination.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Customer {
    private Long customerId;
    private String customerCode;
    private String fullName;
    private LocalDate dateOfBirth;
    private String mobileNumber;
    private String email;
    private String addressLine;
    private String city;
    private String state;
    private String postalCode;
    private String kycType;
    private String kycReference;
    private String kycStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Customer() {
    }

    public Customer(Long customerId, String customerCode, String fullName, LocalDate dateOfBirth,
                    String mobileNumber, String email, String addressLine, String city,
                    String state, String postalCode, String kycType, String kycReference,
                    String kycStatus, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.customerId = customerId;
        this.customerCode = customerCode;
        this.fullName = fullName;
        this.dateOfBirth = dateOfBirth;
        this.mobileNumber = mobileNumber;
        this.email = email;
        this.addressLine = addressLine;
        this.city = city;
        this.state = state;
        this.postalCode = postalCode;
        this.kycType = kycType;
        this.kycReference = kycReference;
        this.kycStatus = kycStatus;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
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

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAddressLine() {
        return addressLine;
    }

    public void setAddressLine(String addressLine) {
        this.addressLine = addressLine;
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

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getKycType() {
        return kycType;
    }

    public void setKycType(String kycType) {
        this.kycType = kycType;
    }

    public String getKycReference() {
        return kycReference;
    }

    public void setKycReference(String kycReference) {
        this.kycReference = kycReference;
    }

    public String getKycStatus() {
        return kycStatus;
    }

    public void setKycStatus(String kycStatus) {
        this.kycStatus = kycStatus;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getFullAddress() {
        StringBuilder sb = new StringBuilder();
        if (addressLine != null && !addressLine.isBlank()) sb.append(addressLine);
        if (city != null && !city.isBlank()) sb.append(", ").append(city);
        if (state != null && !state.isBlank()) sb.append(", ").append(state);
        if (postalCode != null && !postalCode.isBlank()) sb.append(" - ").append(postalCode);
        return sb.toString();
    }
}
