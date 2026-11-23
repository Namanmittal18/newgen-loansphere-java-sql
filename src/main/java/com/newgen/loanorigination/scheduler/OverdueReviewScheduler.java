package com.newgen.loanorigination.scheduler;

import com.newgen.loanorigination.config.BusinessClock;
import com.newgen.loanorigination.dao.*;
import com.newgen.loanorigination.model.Customer;
import com.newgen.loanorigination.model.LoanApplication;
import com.newgen.loanorigination.model.SystemAlert;
import com.newgen.loanorigination.model.enums.AlertType;
import com.newgen.loanorigination.model.enums.ApplicationStatus;
import com.newgen.loanorigination.model.enums.UtilityResult;
import com.newgen.loanorigination.utility.CsvExportUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class OverdueReviewScheduler {

    private static final Logger log = LoggerFactory.getLogger(OverdueReviewScheduler.class);
    private static final DateTimeFormatter FILE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final LoanApplicationDAO loanApplicationDAO;
    private final CustomerDAO customerDAO;
    private final SystemAlertDAO systemAlertDAO;
    private final UtilityRunDAO utilityRunDAO;
    private final BusinessClock businessClock;

    @Value("${loansphere.export.csv.directory:exports}")
    private String exportDirectory;

    @Value("${loansphere.scheduler.enabled:true}")
    private boolean schedulerEnabled;

    public OverdueReviewScheduler(LoanApplicationDAO loanApplicationDAO,
                                  CustomerDAO customerDAO,
                                  SystemAlertDAO systemAlertDAO,
                                  UtilityRunDAO utilityRunDAO,
                                  BusinessClock businessClock) {
        this.loanApplicationDAO = loanApplicationDAO;
        this.customerDAO = customerDAO;
        this.systemAlertDAO = systemAlertDAO;
        this.utilityRunDAO = utilityRunDAO;
        this.businessClock = businessClock;
    }

    /**
     * Automatic scheduled background task that monitors pending loan reviews and exports CSV report.
     */
    @Scheduled(cron = "${loansphere.scheduler.overdue.cron:0 0 0 * * ?}")
    public void executeDailyScheduledTask() {
        if (!schedulerEnabled) {
            log.info("Overdue review scheduler is disabled by configuration.");
            return;
        }
        log.info("Starting scheduled daily overdue review job...");
        runUtilityNow(null);
    }

    /**
     * Executes the overdue review utility with identical business logic.
     * Can be invoked manually from the Web or Console interface.
     *
     * @param overrideDate optional business date override for boundary testing
     * @return summary report of the utility run
     */
    public UtilityExecutionSummary runUtilityNow(LocalDate overrideDate) {
        LocalDate effectiveDate = (overrideDate != null) ? overrideDate : businessClock.getCurrentDate();
        LocalDateTime startTime = businessClock.getCurrentDateTime();

        Long runId = utilityRunDAO.startRun("OVERDUE_REVIEW_JOB", startTime);
        log.info("Overdue review job [RunID: {}] started for effective date: {}", runId, effectiveDate);

        int processedCount = 0;
        int alertsCreatedCount = 0;
        String generatedCsvPath = null;
        String errorMessage = null;
        UtilityResult finalResult = UtilityResult.SUCCESS;

        try {
            // 1. Check pending applications in UNDER_REVIEW
            List<LoanApplication> pendingApps = loanApplicationDAO.findByStatus(ApplicationStatus.UNDER_REVIEW);
            processedCount = pendingApps.size();

            List<List<String>> csvRows = new ArrayList<>();

            for (LoanApplication app : pendingApps) {
                Customer customer = customerDAO.findById(app.getCustomerId()).orElse(null);
                String customerName = customer != null ? customer.getFullName() : "N/A";
                String mobileNumber = customer != null ? customer.getMobileNumber() : "N/A";

                LocalDate submissionDate = (app.getSubmittedAt() != null)
                        ? app.getSubmittedAt().toLocalDate()
                        : app.getCreatedAt().toLocalDate();

                // Calculate calendar days passed since submission
                long calendarDaysPassed = ChronoUnit.DAYS.between(submissionDate, effectiveDate);

                // Overdue rule: only when strictly MORE than 2 calendar days have passed
                // 2 days -> Not overdue; 3 days -> Overdue
                boolean isOverdue = calendarDaysPassed > 2;

                if (isOverdue) {
                    SystemAlert alert = new SystemAlert(
                            null,
                            app.getApplicationId(),
                            AlertType.REVIEW_OVERDUE,
                            startTime
                    );
                    boolean created = systemAlertDAO.saveIfAbsent(alert);
                    if (created) {
                        alertsCreatedCount++;
                        log.info("Created REVIEW_OVERDUE alert for application {}", app.getApplicationNumber());
                    }
                }

                // Add to CSV rows
                List<String> row = List.of(
                        app.getApplicationNumber(),
                        customerName,
                        mobileNumber,
                        app.getProductType().name(),
                        app.getRequestedAmount().toPlainString(),
                        app.getSubmittedAt() != null ? app.getSubmittedAt().format(DISPLAY_FORMATTER) : "",
                        String.valueOf(calendarDaysPassed),
                        isOverdue ? "Yes" : "No"
                );
                csvRows.add(row);
            }

            // 2. Export CSV report: pending_reviews_YYYYMMDD.csv
            List<String> headers = List.of(
                    "ApplicationNumber", "CustomerName", "MobileNumber", "ProductType",
                    "RequestedAmount", "SubmittedAt", "DaysWaiting", "OverdueFlag"
            );

            String fileName = String.format("pending_reviews_%s.csv", effectiveDate.format(FILE_DATE_FORMATTER));
            File exportDir = new File(exportDirectory);
            if (!exportDir.exists()) {
                exportDir.mkdirs();
            }
            File csvFile = new File(exportDir, fileName);
            CsvExportUtility.writeCsv(csvFile, headers, csvRows);
            generatedCsvPath = csvFile.getAbsolutePath();

            log.info("Exported pending reviews CSV to: {}", generatedCsvPath);

        } catch (Exception e) {
            log.error("Error occurred while executing Overdue Review Job: {}", e.getMessage(), e);
            finalResult = UtilityResult.FAILURE;
            errorMessage = e.getMessage();
        } finally {
            LocalDateTime endTime = businessClock.getCurrentDateTime();
            utilityRunDAO.completeRun(runId, finalResult, processedCount, alertsCreatedCount, errorMessage, endTime);
        }

        return new UtilityExecutionSummary(runId, "OVERDUE_REVIEW_JOB", finalResult,
                processedCount, alertsCreatedCount, generatedCsvPath, errorMessage);
    }

    public static class UtilityExecutionSummary {
        private final Long runId;
        private final String utilityName;
        private final UtilityResult result;
        private final int processedCount;
        private final int alertsCreated;
        private final String outputFilePath;
        private final String errorMessage;

        public UtilityExecutionSummary(Long runId, String utilityName, UtilityResult result,
                                       int processedCount, int alertsCreated, String outputFilePath, String errorMessage) {
            this.runId = runId;
            this.utilityName = utilityName;
            this.result = result;
            this.processedCount = processedCount;
            this.alertsCreated = alertsCreated;
            this.outputFilePath = outputFilePath;
            this.errorMessage = errorMessage;
        }

        public Long getRunId() { return runId; }
        public String getUtilityName() { return utilityName; }
        public UtilityResult getResult() { return result; }
        public int getProcessedCount() { return processedCount; }
        public int getAlertsCreated() { return alertsCreated; }
        public String getOutputFilePath() { return outputFilePath; }
        public String getErrorMessage() { return errorMessage; }
    }
}
