package com.newgen.loanorigination.console;

import com.newgen.loanorigination.config.BusinessClock;
import com.newgen.loanorigination.dto.ApplicationSubmissionResponse;
import com.newgen.loanorigination.dto.DisbursementResponse;
import com.newgen.loanorigination.dto.reports.*;
import com.newgen.loanorigination.model.*;
import com.newgen.loanorigination.model.enums.ApplicationStatus;
import com.newgen.loanorigination.model.enums.ProductType;
import com.newgen.loanorigination.scheduler.CoreBankingScheduler;
import com.newgen.loanorigination.scheduler.OverdueReviewScheduler;
import com.newgen.loanorigination.service.*;
import com.newgen.loanorigination.utility.CurrencyFormatter;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;

@Component
public class ConsoleMenuHandler {

    private final CustomerService customerService;
    private final LoanApplicationService loanApplicationService;
    private final ReviewerService reviewerService;
    private final DisbursementService disbursementService;
    private final ReportService reportService;
    private final OverdueReviewScheduler overdueReviewScheduler;
    private final CoreBankingScheduler coreBankingScheduler;
    private final BusinessClock businessClock;
    private final PdfGenerationService pdfGenerationService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public ConsoleMenuHandler(CustomerService customerService,
                              LoanApplicationService loanApplicationService,
                              ReviewerService reviewerService,
                              DisbursementService disbursementService,
                              ReportService reportService,
                              OverdueReviewScheduler overdueReviewScheduler,
                              CoreBankingScheduler coreBankingScheduler,
                              BusinessClock businessClock,
                              PdfGenerationService pdfGenerationService) {
        this.customerService = customerService;
        this.loanApplicationService = loanApplicationService;
        this.reviewerService = reviewerService;
        this.disbursementService = disbursementService;
        this.reportService = reportService;
        this.overdueReviewScheduler = overdueReviewScheduler;
        this.coreBankingScheduler = coreBankingScheduler;
        this.businessClock = businessClock;
        this.pdfGenerationService = pdfGenerationService;
    }

    public void startConsole(Scanner scanner) {
        boolean exit = false;
        while (!exit) {
            printMainMenu();
            System.out.print("Enter your choice (1-13): ");
            String input = scanner.nextLine().trim();

            switch (input) {
                case "1" -> handleCustomerManagement(scanner);
                case "2" -> handleLoanApplicationCreation(scanner);
                case "3" -> handleApplicationSubmission(scanner);
                case "4" -> handleViewApplications(scanner);
                case "5" -> handleReviewerDecisions(scanner);
                case "6" -> handleCreditApproval(scanner);
                case "7" -> handleDisbursement(scanner);
                case "8" -> handleSqlReports(scanner);
                case "9" -> handleRunOverdueUtility(scanner);
                case "10" -> handleRunCoreDataPush(scanner);
                case "11" -> handleViewAlerts();
                case "12" -> handleViewApplicationHistory(scanner);
                case "13" -> {
                    System.out.println("\nThank you for using LoanSphere. Exiting...");
                    exit = true;
                }
                default -> System.out.println("\n[!] Invalid selection. Please enter a number between 1 and 13.");
            }
        }
    }

    private void printMainMenu() {
        System.out.println("\n===============================================================================");
        System.out.println("            LoanSphere | Mini Loan Origination Console Application             ");
        System.out.println("                 Current Business Date: " + businessClock.getCurrentDate());
        System.out.println("===============================================================================");
        System.out.println(" 1. Customer Management (Register / Search / View)");
        System.out.println(" 2. Loan Application Creation (Create DRAFT)");
        System.out.println(" 3. Application Submission (Submit for Automated Eligibility)");
        System.out.println(" 4. View Applications (List All / View Details)");
        System.out.println(" 5. Reviewer Decisions (Approve / Reject Under Review)");
        System.out.println(" 6. Credit Approval (Sanction Management)");
        System.out.println(" 7. Simulated Disbursement (Disburse Approved Loans)");
        System.out.println(" 8. SQL Reports (All 6 Parameterized Reports)");
        System.out.println(" 9. Run Overdue Utility ('Run Utility Now')");
        System.out.println("10. Run CoreDataPush ('Run CoreDataPush Now')");
        System.out.println("11. View System Alerts");
        System.out.println("12. View Application Audit History");
        System.out.println("13. Exit");
        System.out.println("-------------------------------------------------------------------------------");
    }

    // 1. Customer Management
    private void handleCustomerManagement(Scanner scanner) {
        System.out.println("\n--- [1] Customer Management ---");
        System.out.println("1. Register New Customer");
        System.out.println("2. Search Customer (by Name, Mobile, or Code)");
        System.out.println("3. List All Customers");
        System.out.print("Select sub-option (1-3): ");
        String sub = scanner.nextLine().trim();

        if ("1".equals(sub)) {
            try {
                Customer c = new Customer();
                System.out.print("Enter Full Name: ");
                c.setFullName(scanner.nextLine().trim());

                System.out.print("Enter Date of Birth (YYYY-MM-DD): ");
                c.setDateOfBirth(LocalDate.parse(scanner.nextLine().trim(), DATE_FORMATTER));

                System.out.print("Enter 10-digit Mobile Number: ");
                c.setMobileNumber(scanner.nextLine().trim());

                System.out.print("Enter Email Address: ");
                c.setEmail(scanner.nextLine().trim());

                System.out.print("Enter Address Line: ");
                c.setAddressLine(scanner.nextLine().trim());

                System.out.print("Enter City: ");
                c.setCity(scanner.nextLine().trim());

                System.out.print("Enter State: ");
                c.setState(scanner.nextLine().trim());

                System.out.print("Enter Postal Code: ");
                c.setPostalCode(scanner.nextLine().trim());

                System.out.print("Enter KYC Type (AADHAAR / PAN / PASSPORT): ");
                c.setKycType(scanner.nextLine().trim().toUpperCase());

                System.out.print("Enter KYC Reference Number: ");
                c.setKycReference(scanner.nextLine().trim());

                c.setKycStatus("VERIFIED");

                Customer registered = customerService.registerCustomer(c);
                System.out.println("\n[✓] Customer registered successfully!");
                System.out.println("    Customer ID   : " + registered.getCustomerId());
                System.out.println("    Customer Code : " + registered.getCustomerCode());
                System.out.println("    Full Name     : " + registered.getFullName());
                System.out.println("    Mobile        : " + registered.getMobileNumber());
            } catch (Exception e) {
                System.out.println("\n[X] Error registering customer: " + e.getMessage());
            }
        } else if ("2".equals(sub)) {
            System.out.print("Enter search term: ");
            String q = scanner.nextLine().trim();
            List<Customer> results = customerService.searchCustomers(q);
            printCustomerTable(results);
        } else if ("3".equals(sub)) {
            printCustomerTable(customerService.getAllCustomers());
        }
    }

    // 2. Loan Application Creation
    private void handleLoanApplicationCreation(Scanner scanner) {
        System.out.println("\n--- [2] Create DRAFT Loan Application ---");
        try {
            System.out.print("Enter Customer ID: ");
            Long custId = Long.parseLong(scanner.nextLine().trim());

            Customer cust = customerService.getCustomerById(custId);
            System.out.println("Selected Customer: " + cust.getFullName() + " (" + cust.getCustomerCode() + ")");

            System.out.print("Enter Product Type (PERSONAL / EDUCATION): ");
            ProductType productType = ProductType.valueOf(scanner.nextLine().trim().toUpperCase());

            System.out.print("Enter Branch Code (e.g. BR001): ");
            String branchCode = scanner.nextLine().trim();

            System.out.print("Enter Branch Name: ");
            String branchName = scanner.nextLine().trim();

            System.out.print("Enter Requested Loan Amount (₹): ");
            BigDecimal requestedAmount = new BigDecimal(scanner.nextLine().trim());

            System.out.print("Enter Loan Tenure (in Months): ");
            int tenureMonths = Integer.parseInt(scanner.nextLine().trim());

            System.out.print("Enter Monthly Net Income (₹): ");
            BigDecimal netIncome = new BigDecimal(scanner.nextLine().trim());

            System.out.print("Enter Existing Monthly Obligations (₹): ");
            BigDecimal obligations = new BigDecimal(scanner.nextLine().trim());

            LoanApplication app = new LoanApplication();
            app.setCustomerId(custId);
            app.setProductType(productType);
            app.setBranchCode(branchCode);
            app.setBranchName(branchName);
            app.setRequestedAmount(requestedAmount);
            app.setTenureMonths(tenureMonths);
            app.setMonthlyNetIncome(netIncome);
            app.setExistingMonthlyObligations(obligations);

            LoanApplication drafted = loanApplicationService.createDraft(app);
            System.out.println("\n[✓] DRAFT Application Created Successfully!");
            System.out.println("    Application No  : " + drafted.getApplicationNumber());
            System.out.println("    Product         : " + drafted.getProductType());
            System.out.println("    Requested Amount: " + CurrencyFormatter.formatInr(drafted.getRequestedAmount()));
            System.out.println("    Tenure          : " + drafted.getTenureMonths() + " Months");
            System.out.println("    Interest Rate   : " + drafted.getAnnualInterestRate() + "% p.a.");
            System.out.println("    Proposed EMI    : " + CurrencyFormatter.formatInr(drafted.getProposedEmi()));
            System.out.println("    Proposed FOIR   : " + drafted.getFoirPercentage() + "%");
            System.out.println("    Status          : " + drafted.getStatus());
        } catch (Exception e) {
            System.out.println("\n[X] Error creating draft application: " + e.getMessage());
        }
    }

    // 3. Application Submission
    private void handleApplicationSubmission(Scanner scanner) {
        System.out.println("\n--- [3] Submit Loan Application for Eligibility Assessment ---");
        try {
            System.out.print("Enter Application ID or Application Number: ");
            String input = scanner.nextLine().trim();
            LoanApplication app = input.toUpperCase().startsWith("APP-")
                    ? loanApplicationService.getApplicationByNumber(input)
                    : loanApplicationService.getApplicationById(Long.parseLong(input));

            System.out.println("Submitting Application " + app.getApplicationNumber() + " for Customer " +
                    (app.getCustomer() != null ? app.getCustomer().getFullName() : app.getCustomerId()) + "...");

            ApplicationSubmissionResponse response = loanApplicationService.submitApplication(app.getApplicationId());

            System.out.println("\n----------------- Submission Result -----------------");
            System.out.println("Status        : " + response.getStatus());
            System.out.println("Eligible      : " + (response.isEligible() ? "YES" : "NO"));
            System.out.println("Proposed EMI  : " + CurrencyFormatter.formatInr(response.getProposedEmi()));
            System.out.println("Calculated FOIR: " + response.getFoirPercentage() + "% (Threshold: <= 50.00%)");
            System.out.println("Result Message: " + response.getMessage());

            if (!response.isEligible() && response.getFailedReasons() != null && !response.getFailedReasons().isEmpty()) {
                System.out.println("\nFailed Eligibility Rules:");
                for (String reason : response.getFailedReasons()) {
                    System.out.println("  * " + reason);
                }
            }
            System.out.println("-----------------------------------------------------");
        } catch (Exception e) {
            System.out.println("\n[X] Submission failed: " + e.getMessage());
        }
    }

    // 4. View Applications
    private void handleViewApplications(Scanner scanner) {
        System.out.println("\n--- [4] View Loan Applications ---");
        System.out.println("1. List All Applications");
        System.out.println("2. View Specific Application by Number");
        System.out.print("Select sub-option (1-2): ");
        String sub = scanner.nextLine().trim();

        if ("1".equals(sub)) {
            List<LoanApplication> apps = loanApplicationService.getAllApplications();
            printApplicationTable(apps);
        } else if ("2".equals(sub)) {
            System.out.print("Enter Application Number (e.g. APP-20261122-0001): ");
            String appNum = scanner.nextLine().trim();
            try {
                LoanApplication app = loanApplicationService.getApplicationByNumber(appNum);
                printApplicationDetails(app);
            } catch (Exception e) {
                System.out.println("\n[X] " + e.getMessage());
            }
        }
    }

    // 5. Reviewer Decisions
    private void handleReviewerDecisions(Scanner scanner) {
        System.out.println("\n--- [5] Reviewer Workspace ---");
        List<LoanApplication> pending = reviewerService.getPendingReviews();
        if (pending.isEmpty()) {
            System.out.println("No applications currently awaiting review (status: UNDER_REVIEW).");
            return;
        }

        printApplicationTable(pending);

        try {
            System.out.print("\nEnter Application ID to review: ");
            Long appId = Long.parseLong(scanner.nextLine().trim());

            System.out.print("Enter Reviewer Code (e.g. REV-101): ");
            String reviewerCode = scanner.nextLine().trim();

            System.out.print("Enter Decision (1 for APPROVE, 2 for REJECT): ");
            String dec = scanner.nextLine().trim();

            ApplicationStatus targetStatus;
            if ("1".equals(dec)) {
                targetStatus = ApplicationStatus.CREDIT_APPROVAL;
            } else if ("2".equals(dec)) {
                targetStatus = ApplicationStatus.REJECTED;
            } else {
                System.out.println("[!] Invalid decision option.");
                return;
            }

            System.out.print("Enter Decision Remarks/Reason" +
                    (targetStatus == ApplicationStatus.REJECTED ? " (MANDATORY): " : ": "));
            String remarks = scanner.nextLine().trim();

            LoanApplication decided = reviewerService.processReviewDecision(appId, reviewerCode, targetStatus, remarks);
            System.out.println("\n[✓] Decision processed successfully! Application " + decided.getApplicationNumber() +
                    " moved to " + decided.getStatus());
        } catch (Exception e) {
            System.out.println("\n[X] Review decision failed: " + e.getMessage());
        }
    }

    // 6. Credit Approval
    private void handleCreditApproval(Scanner scanner) {
        System.out.println("\n--- [6] Credit Approval Workspace ---");
        List<LoanApplication> list = reviewerService.getCreditApprovalPending();
        if (list.isEmpty()) {
            System.out.println("No applications currently in CREDIT_APPROVAL status.");
            return;
        }

        printApplicationTable(list);

        try {
            System.out.print("\nEnter Application ID for credit review: ");
            Long appId = Long.parseLong(scanner.nextLine().trim());

            System.out.print("Enter Credit Officer Code (e.g. CR-001): ");
            String officerCode = scanner.nextLine().trim();

            System.out.print("Do you want to (1) Sanction for Disbursement, or (2) Reject? ");
            String choice = scanner.nextLine().trim();

            if ("2".equals(choice)) {
                System.out.print("Enter Rejection Reason (MANDATORY): ");
                String reason = scanner.nextLine().trim();
                reviewerService.processReviewDecision(appId, officerCode, ApplicationStatus.REJECTED, reason);
                System.out.println("\n[✓] Application rejected by credit authority.");
            } else {
                System.out.println("\n[i] Application is already approved and ready for simulated disbursement!");
                System.out.print("\nDo you want to export official Loan Application Form PDF? (Y/N): ");
                if ("Y".equalsIgnoreCase(scanner.nextLine().trim())) {
                    try {
                        LoanApplication app = loanApplicationService.getApplicationById(appId);
                        byte[] pdfBytes = pdfGenerationService.generateApplicationFormPdf(app, app.getCustomer());
                        String filename = "ApplicationForm_" + app.getApplicationNumber() + ".pdf";
                        java.nio.file.Files.write(java.nio.file.Path.of(filename), pdfBytes);
                        System.out.println("[✓] PDF Application Form exported successfully to: " + filename);
                    } catch (Exception ex) {
                        System.out.println("[X] Failed to export PDF: " + ex.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("\n[X] Error: " + e.getMessage());
        }
    }

    // 7. Simulated Disbursement
    private void handleDisbursement(Scanner scanner) {
        System.out.println("\n--- [7] Simulated Loan Disbursement ---");
        List<LoanApplication> ready = disbursementService.getApplicationsReadyForDisbursement();
        if (ready.isEmpty()) {
            System.out.println("No applications ready for disbursement (status must be CREDIT_APPROVAL).");
            return;
        }

        printApplicationTable(ready);

        try {
            System.out.print("\nEnter Application ID to disburse: ");
            Long appId = Long.parseLong(scanner.nextLine().trim());

            System.out.print("Enter Performed By (User Code / Name): ");
            String user = scanner.nextLine().trim();

            DisbursementResponse resp = disbursementService.disburseLoan(appId, user);

            System.out.println("\n=======================================================");
            System.out.println("             DISBURSEMENT CONFIRMATION                 ");
            System.out.println("=======================================================");
            System.out.println(" Application Number     : " + resp.getApplicationNumber());
            System.out.println(" Customer Name          : " + resp.getCustomerName());
            System.out.println(" Disbursed Amount       : " + CurrencyFormatter.formatInr(resp.getDisbursedAmount()));
            System.out.println(" Disbursement Reference : " + resp.getDisbursementReference());
            System.out.println(" Disbursement Timestamp : " + resp.getDisbursedDate());
            System.out.println(" Status                 : DISBURSED");
            System.out.println("=======================================================");
        } catch (Exception e) {
            System.out.println("\n[X] Disbursement failed: " + e.getMessage());
        }
    }

    // 8. SQL Reports
    private void handleSqlReports(Scanner scanner) {
        System.out.println("\n--- [8] SQL Reports ---");
        System.out.println("1. Application Register");
        System.out.println("2. Product Pipeline (By Status & Branch)");
        System.out.println("3. Daily / Monthly / Quarterly Disbursement Summary");
        System.out.println("4. Overdue Reviews (> 2 Calendar Days)");
        System.out.println("5. Customers Without Disbursement");
        System.out.println("6. Repeat Applicants (GROUP BY + HAVING)");
        System.out.print("Select report (1-6): ");
        String rep = scanner.nextLine().trim();

        switch (rep) {
            case "1" -> {
                System.out.print("Filter Product (PERSONAL / EDUCATION / ALL): ");
                String p = scanner.nextLine().trim();
                System.out.print("Filter Status (e.g. UNDER_REVIEW / ALL): ");
                String s = scanner.nextLine().trim();
                List<ApplicationRegisterReportDTO> list = reportService.getApplicationRegister(p, s);
                System.out.printf("\n%-20s | %-18s | %-10s | %-14s | %-12s | %-15s\n",
                        "App Number", "Customer Name", "Product", "Requested Amt", "Proposed EMI", "Status");
                System.out.println("-".repeat(95));
                for (ApplicationRegisterReportDTO row : list) {
                    System.out.printf("%-20s | %-18s | %-10s | %-14s | %-12s | %-15s\n",
                            row.getApplicationNumber(), row.getCustomerName(), row.getProductType(),
                            CurrencyFormatter.formatInr(row.getRequestedAmount()),
                            CurrencyFormatter.formatInr(row.getProposedEmi()), row.getStatus());
                }
            }
            case "2" -> {
                System.out.println("\n--- Pipeline by Product and Status ---");
                List<ProductPipelineStatusDTO> listA = reportService.getProductPipelineByStatus();
                System.out.printf("%-12s | %-16s | %-8s | %-16s\n", "Product", "Status", "Count", "Total Requested");
                System.out.println("-".repeat(60));
                for (ProductPipelineStatusDTO row : listA) {
                    System.out.printf("%-12s | %-16s | %-8d | %-16s\n",
                            row.getProductType(), row.getStatus(), row.getApplicationCount(),
                            CurrencyFormatter.formatInr(row.getTotalRequestedAmount()));
                }

                System.out.println("\n--- Pipeline by Branch and Product ---");
                List<ProductPipelineBranchDTO> listB = reportService.getProductPipelineByBranch();
                System.out.printf("%-10s | %-24s | %-10s | %-8s | %-16s\n", "Branch", "Branch Name", "Product", "Count", "Total Requested");
                System.out.println("-".repeat(78));
                for (ProductPipelineBranchDTO row : listB) {
                    System.out.printf("%-10s | %-24s | %-10s | %-8d | %-16s\n",
                            row.getBranchCode(), row.getBranchName(), row.getProductType(),
                            row.getApplicationCount(), CurrencyFormatter.formatInr(row.getTotalRequestedAmount()));
                }
            }
            case "3" -> {
                System.out.print("Select Breakdown (DAILY / MONTHLY / QUARTERLY): ");
                String periodType = scanner.nextLine().trim();
                List<DisbursementSummaryDTO> list = reportService.getDisbursementSummary(null, null, periodType);
                System.out.printf("\n%-12s | %-10s | %-24s | %-8s | %-16s\n",
                        "Period", "Branch", "Branch Name", "Count", "Total Disbursed");
                System.out.println("-".repeat(78));
                for (DisbursementSummaryDTO row : list) {
                    System.out.printf("%-12s | %-10s | %-24s | %-8d | %-16s\n",
                            row.getDisbursementPeriod(), row.getBranchCode(), row.getBranchName(),
                            row.getDisbursementCount(), CurrencyFormatter.formatInr(row.getTotalDisbursedAmount()));
                }
            }
            case "4" -> {
                List<OverdueReviewReportDTO> list = reportService.getOverdueReviews();
                System.out.println("\n--- Overdue Reviews (Current UNDER_REVIEW older than 2 calendar days) ---");
                System.out.printf("%-20s | %-18s | %-10s | %-14s | %-12s\n",
                        "App Number", "Customer Name", "Product", "Requested Amt", "Days Waiting");
                System.out.println("-".repeat(82));
                for (OverdueReviewReportDTO row : list) {
                    System.out.printf("%-20s | %-18s | %-10s | %-14s | %-12d\n",
                            row.getApplicationNumber(), row.getCustomerName(), row.getProductType(),
                            CurrencyFormatter.formatInr(row.getRequestedAmount()), row.getDaysWaiting());
                }
            }
            case "5" -> {
                List<CustomerSummaryReportDTO> list = reportService.getCustomersWithoutDisbursement();
                System.out.println("\n--- Customers with Application(s) but Zero Disbursements ---");
                System.out.printf("%-18s | %-20s | %-12s | %-15s | %-15s\n",
                        "Customer Code", "Customer Name", "Mobile", "City", "State");
                System.out.println("-".repeat(85));
                for (CustomerSummaryReportDTO row : list) {
                    System.out.printf("%-18s | %-20s | %-12s | %-15s | %-15s\n",
                            row.getCustomerCode(), row.getCustomerName(), row.getMobileNumber(),
                            row.getCity(), row.getState());
                }
            }
            case "6" -> {
                List<RepeatApplicantReportDTO> list = reportService.getRepeatApplicants();
                System.out.println("\n--- Repeat Applicants (More than 1 Application) ---");
                System.out.printf("%-18s | %-20s | %-12s | %-10s | %-18s\n",
                        "Customer Code", "Customer Name", "Mobile", "Total Apps", "Total Requested Amt");
                System.out.println("-".repeat(85));
                for (RepeatApplicantReportDTO row : list) {
                    System.out.printf("%-18s | %-20s | %-12s | %-10d | %-18s\n",
                            row.getCustomerCode(), row.getCustomerName(), row.getMobileNumber(),
                            row.getTotalApplications(), CurrencyFormatter.formatInr(row.getTotalRequestedAmount()));
                }
            }
            default -> System.out.println("[!] Invalid report selection.");
        }
    }

    // 9. Run Overdue Utility
    private void handleRunOverdueUtility(Scanner scanner) {
        System.out.println("\n--- [9] Run Overdue Review Utility Now ---");
        System.out.print("Enter simulated business date (YYYY-MM-DD) or press ENTER for today: ");
        String dateStr = scanner.nextLine().trim();
        LocalDate override = null;
        if (!dateStr.isEmpty()) {
            try {
                override = LocalDate.parse(dateStr, DATE_FORMATTER);
            } catch (DateTimeParseException e) {
                System.out.println("[!] Invalid date format. Using system date.");
            }
        }

        OverdueReviewScheduler.UtilityExecutionSummary summary = overdueReviewScheduler.runUtilityNow(override);
        System.out.println("\n[✓] Overdue Review Utility Execution Finished!");
        System.out.println("    Run ID                 : " + summary.getRunId());
        System.out.println("    Result                 : " + summary.getResult());
        System.out.println("    Applications Processed : " + summary.getProcessedCount());
        System.out.println("    Alerts Created         : " + summary.getAlertsCreated());
        System.out.println("    CSV Export File        : " + summary.getOutputFilePath());
    }

    // 10. Run CoreDataPush
    private void handleRunCoreDataPush(Scanner scanner) {
        System.out.println("\n--- [10] Run CoreDataPush Utility Now ---");
        CoreBankingScheduler.CorePushExecutionSummary summary = coreBankingScheduler.runCoreDataPushNow();
        System.out.println("\n[✓] CoreDataPush Worker Finished!");
        System.out.println("    Run ID               : " + summary.getRunId());
        System.out.println("    Result               : " + summary.getResult());
        System.out.println("    Records Claimed      : " + summary.getProcessedCount());
        System.out.println("    Successfully Pushed  : " + summary.getSuccessfulCount());
        if (summary.getErrorMessage() != null) {
            System.out.println("    Errors               : " + summary.getErrorMessage());
        }
    }

    // 11. View System Alerts
    private void handleViewAlerts() {
        System.out.println("\n--- [11] System Alerts ---");
        List<OverdueReviewReportDTO> list = reportService.getOverdueReviews();
        if (list.isEmpty()) {
            System.out.println("No active system alerts.");
            return;
        }
        System.out.printf("%-10s | %-20s | %-20s | %-16s | %-12s\n",
                "Alert ID", "App Number", "Customer Name", "Alert Type", "Days Waiting");
        System.out.println("-".repeat(85));
        for (OverdueReviewReportDTO row : list) {
            System.out.printf("%-10d | %-20s | %-20s | %-16s | %-12d\n",
                    row.getApplicationId(), row.getApplicationNumber(), row.getCustomerName(),
                    "REVIEW_OVERDUE", row.getDaysWaiting());
        }
    }

    // 12. View Application History
    private void handleViewApplicationHistory(Scanner scanner) {
        System.out.println("\n--- [12] View Application Audit History ---");
        System.out.print("Enter Application ID: ");
        try {
            Long appId = Long.parseLong(scanner.nextLine().trim());
            List<ApplicationHistory> history = loanApplicationService.getApplicationHistory(appId);
            if (history.isEmpty()) {
                System.out.println("No history records found for application ID: " + appId);
                return;
            }

            System.out.printf("\n%-6s | %-15s | %-14s | %-16s | %-15s | %-20s | %s\n",
                    "ID", "Action", "Old Status", "New Status", "Performed By", "Timestamp", "Remarks");
            System.out.println("-".repeat(110));
            for (ApplicationHistory h : history) {
                System.out.printf("%-6d | %-15s | %-14s | %-16s | %-15s | %-20s | %s\n",
                        h.getHistoryId(), h.getAction(),
                        h.getOldStatus() != null ? h.getOldStatus() : "-",
                        h.getNewStatus(), h.getPerformedBy(),
                        h.getPerformedAt() != null ? h.getPerformedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "-",
                        h.getRemarks() != null ? h.getRemarks() : "");
            }
        } catch (Exception e) {
            System.out.println("[X] Error: " + e.getMessage());
        }
    }

    private void printCustomerTable(List<Customer> list) {
        if (list.isEmpty()) {
            System.out.println("No customers found.");
            return;
        }
        System.out.printf("\n%-4s | %-18s | %-20s | %-12s | %-12s | %-15s | %-12s\n",
                "ID", "Code", "Full Name", "DOB", "Mobile", "City", "KYC Ref");
        System.out.println("-".repeat(105));
        for (Customer c : list) {
            System.out.printf("%-4d | %-18s | %-20s | %-12s | %-12s | %-15s | %-12s\n",
                    c.getCustomerId(), c.getCustomerCode(), c.getFullName(),
                    c.getDateOfBirth(), c.getMobileNumber(), c.getCity(), c.getKycReference());
        }
    }

    private void printApplicationTable(List<LoanApplication> list) {
        if (list.isEmpty()) {
            System.out.println("No applications found.");
            return;
        }
        System.out.printf("\n%-4s | %-20s | %-18s | %-10s | %-14s | %-6s | %-12s | %-16s\n",
                "ID", "App Number", "Customer Name", "Product", "Amount", "Tenure", "EMI", "Status");
        System.out.println("-".repeat(110));
        for (LoanApplication a : list) {
            String cName = a.getCustomer() != null ? a.getCustomer().getFullName() : "ID: " + a.getCustomerId();
            System.out.printf("%-4d | %-20s | %-18s | %-10s | %-14s | %-6d | %-12s | %-16s\n",
                    a.getApplicationId(), a.getApplicationNumber(), cName, a.getProductType(),
                    CurrencyFormatter.formatInr(a.getRequestedAmount()), a.getTenureMonths(),
                    CurrencyFormatter.formatInr(a.getProposedEmi()), a.getStatus());
        }
    }

    private void printApplicationDetails(LoanApplication app) {
        System.out.println("\n=======================================================");
        System.out.println("             LOAN APPLICATION DETAILS                  ");
        System.out.println("=======================================================");
        System.out.println(" Application ID       : " + app.getApplicationId());
        System.out.println(" Application Number   : " + app.getApplicationNumber());
        System.out.println(" Status               : " + app.getStatus());
        if (app.getCustomer() != null) {
            System.out.println(" Customer Code        : " + app.getCustomer().getCustomerCode());
            System.out.println(" Customer Name        : " + app.getCustomer().getFullName());
            System.out.println(" Mobile Number        : " + app.getCustomer().getMobileNumber());
        }
        System.out.println(" Product Type         : " + app.getProductType());
        System.out.println(" Branch               : " + app.getBranchName() + " (" + app.getBranchCode() + ")");
        System.out.println(" Requested Amount     : " + CurrencyFormatter.formatInr(app.getRequestedAmount()));
        System.out.println(" Tenure               : " + app.getTenureMonths() + " Months");
        System.out.println(" Annual Interest Rate : " + app.getAnnualInterestRate() + "% p.a.");
        System.out.println(" Net Monthly Income   : " + CurrencyFormatter.formatInr(app.getMonthlyNetIncome()));
        System.out.println(" Monthly Obligations  : " + CurrencyFormatter.formatInr(app.getExistingMonthlyObligations()));
        System.out.println(" Calculated EMI       : " + CurrencyFormatter.formatInr(app.getProposedEmi()));
        System.out.println(" Calculated FOIR      : " + app.getFoirPercentage() + "%");
        System.out.println(" Eligible             : " + (app.getEligible() != null && app.getEligible() ? "YES" : "NO"));
        if (app.getEligibilityReasons() != null) {
            System.out.println(" Eligibility Reasons  : " + app.getEligibilityReasons());
        }
        if (app.getReviewerCode() != null) {
            System.out.println(" Reviewer Code        : " + app.getReviewerCode());
            System.out.println(" Decision Reason      : " + app.getDecisionReason());
        }
        if (app.getApprovedAmount() != null) {
            System.out.println(" Approved Amount      : " + CurrencyFormatter.formatInr(app.getApprovedAmount()));
        }
        System.out.println(" Created At           : " + app.getCreatedAt());
        System.out.println(" Submitted At         : " + app.getSubmittedAt());
        System.out.println("=======================================================");
    }
}
