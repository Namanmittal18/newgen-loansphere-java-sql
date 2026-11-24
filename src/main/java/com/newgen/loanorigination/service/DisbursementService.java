package com.newgen.loanorigination.service;

import com.newgen.loanorigination.config.BusinessClock;
import com.newgen.loanorigination.dao.ApplicationHistoryDAO;
import com.newgen.loanorigination.dao.CustomerDAO;
import com.newgen.loanorigination.dao.DisbursementDAO;
import com.newgen.loanorigination.dao.LoanApplicationDAO;
import com.newgen.loanorigination.dto.DisbursementResponse;
import com.newgen.loanorigination.exception.ApplicationNotFoundException;
import com.newgen.loanorigination.exception.CustomerNotFoundException;
import com.newgen.loanorigination.exception.DisbursementException;
import com.newgen.loanorigination.exception.InvalidApplicationStateException;
import com.newgen.loanorigination.model.ApplicationHistory;
import com.newgen.loanorigination.model.Customer;
import com.newgen.loanorigination.model.Disbursement;
import com.newgen.loanorigination.model.LoanApplication;
import com.newgen.loanorigination.model.enums.ApplicationStatus;
import com.newgen.loanorigination.utility.ReferenceGenerator;
import com.newgen.loanorigination.validation.WorkflowValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class DisbursementService {

    private final LoanApplicationDAO loanApplicationDAO;
    private final CustomerDAO customerDAO;
    private final DisbursementDAO disbursementDAO;
    private final ApplicationHistoryDAO historyDAO;
    private final WorkflowValidator workflowValidator;
    private final BusinessClock businessClock;

    public DisbursementService(LoanApplicationDAO loanApplicationDAO,
                               CustomerDAO customerDAO,
                               DisbursementDAO disbursementDAO,
                               ApplicationHistoryDAO historyDAO,
                               WorkflowValidator workflowValidator,
                               BusinessClock businessClock) {
        this.loanApplicationDAO = loanApplicationDAO;
        this.customerDAO = customerDAO;
        this.disbursementDAO = disbursementDAO;
        this.historyDAO = historyDAO;
        this.workflowValidator = workflowValidator;
        this.businessClock = businessClock;
    }

    public List<LoanApplication> getApplicationsReadyForDisbursement() {
        List<LoanApplication> list = loanApplicationDAO.findByStatus(ApplicationStatus.CREDIT_APPROVAL);
        for (LoanApplication app : list) {
            customerDAO.findById(app.getCustomerId()).ifPresent(app::setCustomer);
        }
        return list;
    }

    @Transactional
    public DisbursementResponse disburseLoan(Long applicationId, String performedBy) {
        LoanApplication app = loanApplicationDAO.findById(applicationId)
                .orElseThrow(() -> new ApplicationNotFoundException("Application not found with ID: " + applicationId));

        // Check for repeated disbursement first
        if (disbursementDAO.existsByApplicationId(applicationId)) {
            throw new DisbursementException("Repeated disbursement rejected: Loan application " +
                    app.getApplicationNumber() + " has already been disbursed.");
        }

        if (app.getStatus() != ApplicationStatus.CREDIT_APPROVAL) {
            throw new InvalidApplicationStateException(String.format(
                    "Only applications in CREDIT_APPROVAL status can be disbursed. Current status: %s", app.getStatus()));
        }

        Customer customer = customerDAO.findById(app.getCustomerId())
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found: " + app.getCustomerId()));

        LocalDate businessDate = businessClock.getCurrentDate();
        LocalDateTime now = businessClock.getCurrentDateTime();

        workflowValidator.validateTransition(app.getStatus(), ApplicationStatus.DISBURSED);

        // Generate unique disbursement reference
        String disbRef = ReferenceGenerator.generateDisbursementReference(businessDate);
        if (disbursementDAO.existsByReference(disbRef)) {
            throw new DisbursementException("Generated duplicate disbursement reference: " + disbRef);
        }

        BigDecimal amountToDisburse = app.getApprovedAmount() != null ? app.getApprovedAmount() : app.getRequestedAmount();

        // 1. Create and save disbursement record
        Disbursement disbursement = new Disbursement();
        disbursement.setApplicationId(applicationId);
        disbursement.setDisbursementReference(disbRef);
        disbursement.setAmount(amountToDisburse);
        disbursement.setDisbursedAt(now);
        disbursementDAO.save(disbursement);

        // 2. Update loan application status to DISBURSED
        app.setStatus(ApplicationStatus.DISBURSED);
        loanApplicationDAO.update(app);

        // 3. Record application audit history
        String user = (performedBy != null && !performedBy.isBlank()) ? performedBy.trim() : "OPS_DISBURSEMENT_USER";
        ApplicationHistory history = new ApplicationHistory(
                null,
                applicationId,
                "DISBURSE",
                ApplicationStatus.CREDIT_APPROVAL.name(),
                ApplicationStatus.DISBURSED.name(),
                user,
                now,
                String.format("Full loan amount ₹%s disbursed successfully with reference %s.",
                        amountToDisburse.toPlainString(), disbRef)
        );
        historyDAO.save(history);

        return new DisbursementResponse(
                app.getApplicationNumber(),
                customer.getFullName(),
                amountToDisburse,
                disbRef,
                now
        );
    }

    public List<Disbursement> getAllDisbursements() {
        return disbursementDAO.findAll();
    }

    public BigDecimal getTotalDisbursedAmount() {
        return disbursementDAO.sumTotalDisbursedAmount();
    }

    public long getTotalDisbursementCount() {
        return disbursementDAO.countTotal();
    }
}
