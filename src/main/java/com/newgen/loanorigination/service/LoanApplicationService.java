package com.newgen.loanorigination.service;

import com.newgen.loanorigination.config.BusinessClock;
import com.newgen.loanorigination.config.LoanProductConfig;
import com.newgen.loanorigination.dao.ApplicationHistoryDAO;
import com.newgen.loanorigination.dao.CustomerDAO;
import com.newgen.loanorigination.dao.LoanApplicationDAO;
import com.newgen.loanorigination.dto.ApplicationSubmissionResponse;
import com.newgen.loanorigination.dto.EligibilityCalculationResult;
import com.newgen.loanorigination.exception.ApplicationNotFoundException;
import com.newgen.loanorigination.exception.CustomerNotFoundException;
import com.newgen.loanorigination.exception.DuplicateApplicationException;
import com.newgen.loanorigination.exception.InvalidApplicationStateException;
import com.newgen.loanorigination.exception.ValidationException;
import com.newgen.loanorigination.model.ApplicationHistory;
import com.newgen.loanorigination.model.Customer;
import com.newgen.loanorigination.model.LoanApplication;
import com.newgen.loanorigination.model.enums.ApplicationStatus;
import com.newgen.loanorigination.utility.ReferenceGenerator;
import com.newgen.loanorigination.validation.CustomerValidator;
import com.newgen.loanorigination.validation.ProductValidator;
import com.newgen.loanorigination.validation.WorkflowValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class LoanApplicationService {

    private final LoanApplicationDAO loanApplicationDAO;
    private final CustomerDAO customerDAO;
    private final ApplicationHistoryDAO historyDAO;
    private final FinancialCalculationService financialCalculationService;
    private final CustomerValidator customerValidator;
    private final ProductValidator productValidator;
    private final WorkflowValidator workflowValidator;
    private final LoanProductConfig productConfig;
    private final BusinessClock businessClock;

    public LoanApplicationService(LoanApplicationDAO loanApplicationDAO,
                                  CustomerDAO customerDAO,
                                  ApplicationHistoryDAO historyDAO,
                                  FinancialCalculationService financialCalculationService,
                                  CustomerValidator customerValidator,
                                  ProductValidator productValidator,
                                  WorkflowValidator workflowValidator,
                                  LoanProductConfig productConfig,
                                  BusinessClock businessClock) {
        this.loanApplicationDAO = loanApplicationDAO;
        this.customerDAO = customerDAO;
        this.historyDAO = historyDAO;
        this.financialCalculationService = financialCalculationService;
        this.customerValidator = customerValidator;
        this.productValidator = productValidator;
        this.workflowValidator = workflowValidator;
        this.productConfig = productConfig;
        this.businessClock = businessClock;
    }

    @Transactional
    public LoanApplication createDraft(LoanApplication application) {
        if (application.getCustomerId() == null) {
            throw new ValidationException("Customer ID is required to create a loan application.");
        }

        Customer customer = customerDAO.findById(application.getCustomerId())
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found with ID: " + application.getCustomerId()));

        if (application.getProductType() == null) {
            throw new ValidationException("Product type is required.");
        }

        // Active application rule: only one active application per customer per product
        if (loanApplicationDAO.hasActiveApplication(customer.getCustomerId(), application.getProductType())) {
            throw new DuplicateApplicationException(String.format(
                    "Customer already has an active %s application in DRAFT, UNDER_REVIEW, or CREDIT_APPROVAL.",
                    application.getProductType()));
        }

        LocalDate currentDate = businessClock.getCurrentDate();
        LocalDateTime currentDateTime = businessClock.getCurrentDateTime();

        application.setApplicationNumber(ReferenceGenerator.generateApplicationNumber(currentDate));
        application.setStatus(ApplicationStatus.DRAFT);
        application.setCreatedAt(currentDateTime);

        // Fetch annual interest rate from product configuration
        BigDecimal rate = productConfig.getRule(application.getProductType()).getAnnualInterestRate();
        application.setAnnualInterestRate(rate);

        // Initial preview calculations
        if (application.getRequestedAmount() != null && application.getTenureMonths() != null) {
            BigDecimal emi = financialCalculationService.calculateEmi(
                    application.getRequestedAmount(), rate, application.getTenureMonths());
            application.setProposedEmi(emi);

            if (application.getMonthlyNetIncome() != null && application.getMonthlyNetIncome().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal foir = financialCalculationService.calculateFoir(
                        application.getExistingMonthlyObligations(), emi, application.getMonthlyNetIncome());
                application.setFoirPercentage(foir);
            } else {
                application.setFoirPercentage(BigDecimal.ZERO);
            }
        } else {
            application.setProposedEmi(BigDecimal.ZERO);
            application.setFoirPercentage(BigDecimal.ZERO);
        }

        application.setEligible(false);
        LoanApplication saved = loanApplicationDAO.save(application);

        // Record history
        ApplicationHistory history = new ApplicationHistory(
                null,
                saved.getApplicationId(),
                "CREATE",
                null,
                ApplicationStatus.DRAFT.name(),
                "CUSTOMER",
                currentDateTime,
                "Loan application drafted."
        );
        historyDAO.save(history);

        saved.setCustomer(customer);
        return saved;
    }

    @Transactional
    public LoanApplication updateDraft(LoanApplication updated) {
        LoanApplication existing = getApplicationById(updated.getApplicationId());

        if (existing.getStatus() != ApplicationStatus.DRAFT) {
            throw new InvalidApplicationStateException("Only applications in DRAFT status can be modified.");
        }

        existing.setProductType(updated.getProductType());
        existing.setBranchCode(updated.getBranchCode());
        existing.setBranchName(updated.getBranchName());
        existing.setRequestedAmount(updated.getRequestedAmount());
        existing.setTenureMonths(updated.getTenureMonths());
        existing.setMonthlyNetIncome(updated.getMonthlyNetIncome());
        existing.setExistingMonthlyObligations(updated.getExistingMonthlyObligations());

        BigDecimal rate = productConfig.getRule(existing.getProductType()).getAnnualInterestRate();
        existing.setAnnualInterestRate(rate);

        BigDecimal emi = financialCalculationService.calculateEmi(
                existing.getRequestedAmount(), rate, existing.getTenureMonths());
        existing.setProposedEmi(emi);

        if (existing.getMonthlyNetIncome() != null && existing.getMonthlyNetIncome().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal foir = financialCalculationService.calculateFoir(
                    existing.getExistingMonthlyObligations(), emi, existing.getMonthlyNetIncome());
            existing.setFoirPercentage(foir);
        }

        loanApplicationDAO.update(existing);
        return existing;
    }

    @Transactional
    public ApplicationSubmissionResponse submitApplication(Long applicationId) {
        LoanApplication app = getApplicationById(applicationId);

        if (app.getStatus() != ApplicationStatus.DRAFT) {
            throw new InvalidApplicationStateException("Only DRAFT applications can be submitted. Current status: " + app.getStatus());
        }

        Customer customer = customerDAO.findById(app.getCustomerId())
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found: " + app.getCustomerId()));

        LocalDate businessDate = businessClock.getCurrentDate();
        LocalDateTime now = businessClock.getCurrentDateTime();

        List<String> failedRules = new ArrayList<>();

        // 1. Customer age validation on submission date (21-60 completed years)
        try {
            customerValidator.validateDateOfBirth(customer.getDateOfBirth(), businessDate);
        } catch (ValidationException ve) {
            failedRules.add("AGE_CHECK_FAILED: " + ve.getMessage());
        }

        // 2. Customer mobile validation
        try {
            customerValidator.validateMobile(customer.getMobileNumber());
        } catch (ValidationException ve) {
            failedRules.add("MOBILE_CHECK_FAILED: " + ve.getMessage());
        }

        // 3. Product-specific boundary rules validation
        List<String> productFailures = productValidator.validateRules(app);
        failedRules.addAll(productFailures);

        // 4. Financial calculation & FOIR evaluation
        BigDecimal rate = productConfig.getRule(app.getProductType()).getAnnualInterestRate();
        app.setAnnualInterestRate(rate);

        EligibilityCalculationResult evalResult = financialCalculationService.evaluateEligibility(
                app.getRequestedAmount(),
                rate,
                app.getTenureMonths(),
                app.getExistingMonthlyObligations(),
                app.getMonthlyNetIncome(),
                failedRules
        );

        app.setProposedEmi(evalResult.getProposedEmi());
        app.setFoirPercentage(evalResult.getFoirPercentage());
        app.setSubmittedAt(now);

        ApplicationStatus targetStatus;
        String action;
        String remarks;

        if (evalResult.isEligible()) {
            targetStatus = ApplicationStatus.UNDER_REVIEW;
            workflowValidator.validateTransition(app.getStatus(), targetStatus);

            app.setStatus(targetStatus);
            app.setEligible(true);
            app.setEligibilityReasons("All eligibility and policy rules passed successfully.");
            action = "SUBMIT";
            remarks = String.format("Passed automated eligibility. EMI: ₹%s, FOIR: %s%%.",
                    evalResult.getProposedEmi().toPlainString(), evalResult.getFoirPercentage().toPlainString());
        } else {
            targetStatus = ApplicationStatus.REJECTED;
            workflowValidator.validateTransition(app.getStatus(), targetStatus);

            app.setStatus(targetStatus);
            app.setEligible(false);
            String reasonsSummary = String.join(" | ", evalResult.getFailureReasons());
            app.setEligibilityReasons(reasonsSummary);
            app.setDecidedAt(now);
            app.setReviewerCode("SYSTEM_RULE_ENGINE");
            app.setDecisionReason(reasonsSummary);
            action = "SUBMIT_REJECTED";
            remarks = "Automated submission rejection: " + reasonsSummary;
        }

        loanApplicationDAO.update(app);

        // Record history inside transaction
        ApplicationHistory history = new ApplicationHistory(
                null,
                app.getApplicationId(),
                action,
                ApplicationStatus.DRAFT.name(),
                targetStatus.name(),
                "CUSTOMER",
                now,
                remarks
        );
        historyDAO.save(history);

        String message = evalResult.isEligible()
                ? "Application submitted successfully and moved to UNDER_REVIEW."
                : "Application was rejected due to eligibility policy failures.";

        return ApplicationSubmissionResponse.from(app, evalResult.getFailureReasons(), message);
    }

    public LoanApplication getApplicationById(Long applicationId) {
        LoanApplication app = loanApplicationDAO.findById(applicationId)
                .orElseThrow(() -> new ApplicationNotFoundException("Loan application not found with ID: " + applicationId));
        customerDAO.findById(app.getCustomerId()).ifPresent(app::setCustomer);
        return app;
    }

    public LoanApplication getApplicationByNumber(String applicationNumber) {
        LoanApplication app = loanApplicationDAO.findByApplicationNumber(applicationNumber)
                .orElseThrow(() -> new ApplicationNotFoundException("Loan application not found with number: " + applicationNumber));
        customerDAO.findById(app.getCustomerId()).ifPresent(app::setCustomer);
        return app;
    }

    public List<LoanApplication> getApplicationsByCustomerId(Long customerId) {
        List<LoanApplication> list = loanApplicationDAO.findByCustomerId(customerId);
        customerDAO.findById(customerId).ifPresent(c -> list.forEach(a -> a.setCustomer(c)));
        return list;
    }

    public List<LoanApplication> getApplicationsByStatus(ApplicationStatus status) {
        List<LoanApplication> list = loanApplicationDAO.findByStatus(status);
        populateCustomers(list);
        return list;
    }

    public List<LoanApplication> getAllApplications() {
        List<LoanApplication> list = loanApplicationDAO.findAll();
        populateCustomers(list);
        return list;
    }

    public List<ApplicationHistory> getApplicationHistory(Long applicationId) {
        return historyDAO.findByApplicationId(applicationId);
    }

    private void populateCustomers(List<LoanApplication> applications) {
        for (LoanApplication app : applications) {
            customerDAO.findById(app.getCustomerId()).ifPresent(app::setCustomer);
        }
    }
}
