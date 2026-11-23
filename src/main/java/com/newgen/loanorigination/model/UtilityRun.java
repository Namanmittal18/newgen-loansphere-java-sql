package com.newgen.loanorigination.model;

import com.newgen.loanorigination.model.enums.UtilityResult;

import java.time.LocalDateTime;

public class UtilityRun {
    private Long runId;
    private String utilityName;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private UtilityResult result;
    private Integer applicationsProcessed;
    private Integer alertsCreated;
    private String errorMessage;

    public UtilityRun() {
    }

    public UtilityRun(Long runId, String utilityName, LocalDateTime startedAt, LocalDateTime completedAt,
                      UtilityResult result, Integer applicationsProcessed, Integer alertsCreated, String errorMessage) {
        this.runId = runId;
        this.utilityName = utilityName;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.result = result;
        this.applicationsProcessed = applicationsProcessed;
        this.alertsCreated = alertsCreated;
        this.errorMessage = errorMessage;
    }

    public Long getRunId() {
        return runId;
    }

    public void setRunId(Long runId) {
        this.runId = runId;
    }

    public String getUtilityName() {
        return utilityName;
    }

    public void setUtilityName(String utilityName) {
        this.utilityName = utilityName;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public UtilityResult getResult() {
        return result;
    }

    public void setResult(UtilityResult result) {
        this.result = result;
    }

    public Integer getApplicationsProcessed() {
        return applicationsProcessed;
    }

    public void setApplicationsProcessed(Integer applicationsProcessed) {
        this.applicationsProcessed = applicationsProcessed;
    }

    public Integer getAlertCreated() {
        return alertsCreated;
    }

    public void setAlertsCreated(Integer alertsCreated) {
        this.alertsCreated = alertsCreated;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
