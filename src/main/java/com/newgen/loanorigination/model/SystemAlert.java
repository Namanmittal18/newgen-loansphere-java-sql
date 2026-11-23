package com.newgen.loanorigination.model;

import com.newgen.loanorigination.model.enums.AlertType;

import java.time.LocalDateTime;

public class SystemAlert {
    private Long alertId;
    private Long applicationId;
    private AlertType alertType;
    private LocalDateTime createdAt;

    // Transient fields for display
    private String applicationNumber;
    private String customerName;

    public SystemAlert() {
    }

    public SystemAlert(Long alertId, Long applicationId, AlertType alertType, LocalDateTime createdAt) {
        this.alertId = alertId;
        this.applicationId = applicationId;
        this.alertType = alertType;
        this.createdAt = createdAt;
    }

    public Long getAlertId() {
        return alertId;
    }

    public void setAlertId(Long alertId) {
        this.alertId = alertId;
    }

    public Long getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(Long applicationId) {
        this.applicationId = applicationId;
    }

    public AlertType getAlertType() {
        return alertType;
    }

    public void setAlertType(AlertType alertType) {
        this.alertType = alertType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
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
}
