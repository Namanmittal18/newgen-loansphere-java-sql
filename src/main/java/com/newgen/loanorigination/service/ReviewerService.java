package com.newgen.loanorigination.service;

import com.newgen.loanorigination.config.BusinessClock;
import com.newgen.loanorigination.dao.ApplicationHistoryDAO;
import com.newgen.loanorigination.dao.CustomerDAO;
import com.newgen.loanorigination.dao.LoanApplicationDAO;
import com.newgen.loanorigination.exception.ApplicationNotFoundException;
import com.newgen.loanorigination.exception.InvalidApplicationStateException;
import com.newgen.loanorigination.exception.ValidationException;
import com.newgen.loanorigination.model.ApplicationHistory;
import com.newgen.loanorigination.model.LoanApplication;
import com.newgen.loanorigination.model.enums.ApplicationStatus;
import com.newgen.loanorigination.validation.WorkflowValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReviewerService {

    private final LoanApplicationDAO loanApplicationDAO;
    private final CustomerDAO customerDAO;
    private final ApplicationHistoryDAO historyDAO;
    private final WorkflowValidator workflowValidator;
    private final BusinessClock businessClock;

    public ReviewerService(LoanApplicationDAO loanApplicationDAO,
                           CustomerDAO customerDAO,
                           ApplicationHistoryDAO historyDAO,
                           WorkflowValidator workflowValidator,
                           BusinessClock businessClock) {
        this.loanApplicationDAO = loanApplicationDAO;
        this.customerDAO = customerDAO;
        this.historyDAO = historyDAO;
        this.workflowValidator = workflowValidator;
        this.businessClock = businessClock;
    }

    public List<LoanApplication> getPendingReviews() {
        List<LoanApplication> list = loanApplicationDAO.findByStatus(ApplicationStatus.UNDER_REVIEW);
        for (LoanApplication app : list) {
            customerDAO.findById(app.getCustomerId()).ifPresent(app::setCustomer);
        }
        return list;
    }

    public List<LoanApplication> getCreditApprovalPending() {
        List<LoanApplication> list = loanApplicationDAO.findByStatus(ApplicationStatus.CREDIT_APPROVAL);
        for (LoanApplication app : list) {
            customerDAO.findById(app.getCustomerId()).ifPresent(app::setCustomer);
        }
        return list;
    }

    @Transactional
    public LoanApplication processReviewDecision(Long applicationId, String reviewerCode,
                                                 ApplicationStatus decision, String remarks) {
        if (reviewerCode == null || reviewerCode.trim().isEmpty()) {
            throw new ValidationException("Reviewer code is required to record a decision.");
        }

        LoanApplication app = loanApplicationDAO.findById(applicationId)
                .orElseThrow(() -> new ApplicationNotFoundException("Application not found with ID: " + applicationId));

        if (app.getStatus() != ApplicationStatus.UNDER_REVIEW && app.getStatus() != ApplicationStatus.CREDIT_APPROVAL) {
            throw new InvalidApplicationStateException(String.format(
                    "Reviewer decision cannot be processed on application with status %s.", app.getStatus()));
        }

        workflowValidator.validateTransition(app.getStatus(), decision);
        workflowValidator.validateRejectionReason(decision, remarks);

        LocalDateTime now = businessClock.getCurrentDateTime();
        ApplicationStatus oldStatus = app.getStatus();

        app.setStatus(decision);
        app.setDecidedAt(now);
        app.setReviewerCode(reviewerCode.trim());
        app.setDecisionReason(remarks != null ? remarks.trim() : null);

        String action;
        if (decision == ApplicationStatus.CREDIT_APPROVAL) {
            action = "APPROVE";
            // Approved loan preserves requested amount and tenure
            app.setApprovedAmount(app.getRequestedAmount());
            app.setApprovedAt(now);
        } else if (decision == ApplicationStatus.REJECTED) {
            action = "REJECT";
        } else {
            action = "UPDATE_STATUS";
        }

        loanApplicationDAO.update(app);

        // Record history within transaction
        ApplicationHistory history = new ApplicationHistory(
                null,
                app.getApplicationId(),
                action,
                oldStatus.name(),
                decision.name(),
                reviewerCode.trim(),
                now,
                remarks != null ? remarks.trim() : "Reviewer decision: " + decision.name()
        );
        historyDAO.save(history);

        customerDAO.findById(app.getCustomerId()).ifPresent(app::setCustomer);
        return app;
    }
}
