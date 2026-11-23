package com.newgen.loanorigination.model;

import com.newgen.loanorigination.model.enums.ApplicationStatus;
import com.newgen.loanorigination.model.enums.ProductType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class LoanApplication {
    private Long applicationId;
    private String applicationNumber;
    private Long customerId;
    private ProductType productType;
    private String branchCode;
    private String branchName;
    private BigDecimal requestedAmount;
    private Integer tenureMonths;
    private BigDecimal monthlyNetIncome;
    private BigDecimal existingMonthlyObligations;
    private BigDecimal annualInterestRate;
    private BigDecimal proposedEmi;
    private BigDecimal foirPercentage;
    private Boolean eligible;
    private String eligibilityReasons;
    private ApplicationStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime submittedAt;
    private LocalDateTime decidedAt;
    private String reviewerCode;
    private String decisionReason;
    private BigDecimal approvedAmount;
    private LocalDateTime approvedAt;
    private LocalDateTime updatedAt;

    // Transient fields for display convenience
    private Customer customer;

    public LoanApplication() {
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

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public ProductType getProductType() {
        return productType;
    }

    public void setProductType(ProductType productType) {
        this.productType = productType;
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

    public BigDecimal getRequestedAmount() {
        return requestedAmount;
    }

    public void setRequestedAmount(BigDecimal requestedAmount) {
        this.requestedAmount = requestedAmount;
    }

    public Integer getTenureMonths() {
        return tenureMonths;
    }

    public void setTenureMonths(Integer tenureMonths) {
        this.tenureMonths = tenureMonths;
    }

    public BigDecimal getMonthlyNetIncome() {
        return monthlyNetIncome;
    }

    public void setMonthlyNetIncome(BigDecimal monthlyNetIncome) {
        this.monthlyNetIncome = monthlyNetIncome;
    }

    public BigDecimal getExistingMonthlyObligations() {
        return existingMonthlyObligations;
    }

    public void setExistingMonthlyObligations(BigDecimal existingMonthlyObligations) {
        this.existingMonthlyObligations = existingMonthlyObligations;
    }

    public BigDecimal getAnnualInterestRate() {
        return annualInterestRate;
    }

    public void setAnnualInterestRate(BigDecimal annualInterestRate) {
        this.annualInterestRate = annualInterestRate;
    }

    public BigDecimal getProposedEmi() {
        return proposedEmi;
    }

    public void setProposedEmi(BigDecimal proposedEmi) {
        this.proposedEmi = proposedEmi;
    }

    public BigDecimal getFoirPercentage() {
        return foirPercentage;
    }

    public void setFoirPercentage(BigDecimal foirPercentage) {
        this.foirPercentage = foirPercentage;
    }

    public Boolean getEligible() {
        return eligible;
    }

    public void setEligible(Boolean eligible) {
        this.eligible = eligible;
    }

    public String getEligibilityReasons() {
        return eligibilityReasons;
    }

    public void setEligibilityReasons(String eligibilityReasons) {
        this.eligibilityReasons = eligibilityReasons;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    public void setStatus(ApplicationStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public LocalDateTime getDecidedAt() {
        return decidedAt;
    }

    public void setDecidedAt(LocalDateTime decidedAt) {
        this.decidedAt = decidedAt;
    }

    public String getReviewerCode() {
        return reviewerCode;
    }

    public void setReviewerCode(String reviewerCode) {
        this.reviewerCode = reviewerCode;
    }

    public String getDecisionReason() {
        return decisionReason;
    }

    public void setDecisionReason(String decisionReason) {
        this.decisionReason = decisionReason;
    }

    public BigDecimal getApprovedAmount() {
        return approvedAmount;
    }

    public void setApprovedAmount(BigDecimal approvedAmount) {
        this.approvedAmount = approvedAmount;
    }

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(LocalDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }
}
