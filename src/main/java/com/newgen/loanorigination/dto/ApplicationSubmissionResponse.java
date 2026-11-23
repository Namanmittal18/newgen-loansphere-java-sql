package com.newgen.loanorigination.dto;

import com.newgen.loanorigination.model.LoanApplication;
import com.newgen.loanorigination.model.enums.ApplicationStatus;

import java.math.BigDecimal;
import java.util.List;

public class ApplicationSubmissionResponse {
    private final Long applicationId;
    private final String applicationNumber;
    private final ApplicationStatus status;
    private final boolean eligible;
    private final BigDecimal proposedEmi;
    private final BigDecimal foirPercentage;
    private final List<String> failedReasons;
    private final String message;

    public ApplicationSubmissionResponse(Long applicationId, String applicationNumber, ApplicationStatus status,
                                         boolean eligible, BigDecimal proposedEmi, BigDecimal foirPercentage,
                                         List<String> failedReasons, String message) {
        this.applicationId = applicationId;
        this.applicationNumber = applicationNumber;
        this.status = status;
        this.eligible = eligible;
        this.proposedEmi = proposedEmi;
        this.foirPercentage = foirPercentage;
        this.failedReasons = failedReasons;
        this.message = message;
    }

    public static ApplicationSubmissionResponse from(LoanApplication app, List<String> failedReasons, String message) {
        return new ApplicationSubmissionResponse(
                app.getApplicationId(),
                app.getApplicationNumber(),
                app.getStatus(),
                app.getEligible() != null && app.getEligible(),
                app.getProposedEmi(),
                app.getFoirPercentage(),
                failedReasons,
                message
        );
    }

    public Long getApplicationId() {
        return applicationId;
    }

    public String getApplicationNumber() {
        return applicationNumber;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    public boolean isEligible() {
        return eligible;
    }

    public BigDecimal getProposedEmi() {
        return proposedEmi;
    }

    public BigDecimal getFoirPercentage() {
        return foirPercentage;
    }

    public List<String> getFailedReasons() {
        return failedReasons;
    }

    public String getMessage() {
        return message;
    }
}
