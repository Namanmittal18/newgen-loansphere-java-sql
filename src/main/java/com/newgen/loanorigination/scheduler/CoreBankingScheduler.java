package com.newgen.loanorigination.scheduler;

import com.newgen.loanorigination.config.BusinessClock;
import com.newgen.loanorigination.dao.*;
import com.newgen.loanorigination.model.*;
import com.newgen.loanorigination.model.enums.ApplicationStatus;
import com.newgen.loanorigination.model.enums.UtilityResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CoreBankingScheduler {

    private static final Logger log = LoggerFactory.getLogger(CoreBankingScheduler.class);

    private final LoanApplicationDAO loanApplicationDAO;
    private final CustomerDAO customerDAO;
    private final DisbursementDAO disbursementDAO;
    private final CoreBankingDAO coreBankingDAO;
    private final ApplicationHistoryDAO historyDAO;
    private final UtilityRunDAO utilityRunDAO;
    private final TransactionTemplate transactionTemplate;
    private final BusinessClock businessClock;

    @Value("${loansphere.scheduler.enabled:true}")
    private boolean schedulerEnabled;

    public CoreBankingScheduler(LoanApplicationDAO loanApplicationDAO,
                                CustomerDAO customerDAO,
                                DisbursementDAO disbursementDAO,
                                CoreBankingDAO coreBankingDAO,
                                ApplicationHistoryDAO historyDAO,
                                UtilityRunDAO utilityRunDAO,
                                PlatformTransactionManager transactionManager,
                                BusinessClock businessClock) {
        this.loanApplicationDAO = loanApplicationDAO;
        this.customerDAO = customerDAO;
        this.disbursementDAO = disbursementDAO;
        this.coreBankingDAO = coreBankingDAO;
        this.historyDAO = historyDAO;
        this.utilityRunDAO = utilityRunDAO;
        this.transactionTemplate = new TransactionTemplate(transactionManager,
                new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRES_NEW));
        this.businessClock = businessClock;
    }

    /**
     * Continuously running background task syncing DISBURSED loans into NG_CORE_BANKING.
     */
    @Scheduled(fixedDelayString = "${loansphere.scheduler.corepush.fixed-delay-ms:5000}")
    public void executeContinuousSync() {
        if (!schedulerEnabled) {
            return;
        }
        runCoreDataPushNow();
    }

    /**
     * Executes the CoreDataPush worker synchronously.
     * Can be invoked manually from Web UI or Console.
     *
     * @return summary of the core push run
     */
    public CorePushExecutionSummary runCoreDataPushNow() {
        LocalDateTime startTime = businessClock.getCurrentDateTime();
        List<LoanApplication> disbursedApps = loanApplicationDAO.findDisbursedPendingPush();

        if (disbursedApps.isEmpty()) {
            return new CorePushExecutionSummary(null, UtilityResult.SUCCESS, 0, 0, null);
        }

        Long runId = utilityRunDAO.startRun("CORE_DATA_PUSH_JOB", startTime);
        log.info("CoreDataPush worker [RunID: {}] processing {} disbursed records...", runId, disbursedApps.size());

        int processed = 0;
        int successful = 0;
        String errorMessage = null;
        UtilityResult finalResult = UtilityResult.SUCCESS;

        for (LoanApplication app : disbursedApps) {
            processed++;
            try {
                boolean pushed = processSingleCoreBankingPush(app);
                if (pushed) {
                    successful++;
                }
            } catch (Exception e) {
                log.error("Failed to push application {} to NG_CORE_BANKING: {}",
                        app.getApplicationNumber(), e.getMessage(), e);
                errorMessage = e.getMessage();
                finalResult = UtilityResult.FAILURE;
                // Leave status as DISBURSED so it can be retried without data loss!
            }
        }

        LocalDateTime endTime = businessClock.getCurrentDateTime();
        utilityRunDAO.completeRun(runId, finalResult, processed, 0, errorMessage, endTime);

        return new CorePushExecutionSummary(runId, finalResult, processed, successful, errorMessage);
    }

    /**
     * Pushes a single application into NG_CORE_BANKING and moves its status to EXIT atomically.
     */
    public boolean processSingleCoreBankingPush(LoanApplication app) {
        return Boolean.TRUE.equals(transactionTemplate.execute(status -> {
            // Check if already pushed (prevent duplicates)
            if (coreBankingDAO.existsByApplicationId(app.getApplicationId())) {
                log.warn("Application {} already exists in NG_CORE_BANKING. Moving directly to EXIT.",
                        app.getApplicationNumber());
                app.setStatus(ApplicationStatus.EXIT);
                loanApplicationDAO.updateStatus(app.getApplicationId(), ApplicationStatus.EXIT);
                return true;
            }

            Customer customer = customerDAO.findById(app.getCustomerId())
                    .orElseThrow(() -> new IllegalStateException("Customer not found for application: " + app.getApplicationId()));

            Disbursement disbursement = disbursementDAO.findByApplicationId(app.getApplicationId())
                    .orElseThrow(() -> new IllegalStateException("Disbursement record not found for application: " + app.getApplicationId()));

            LocalDateTime now = businessClock.getCurrentDateTime();

            // 1. Insert into NG_CORE_BANKING
            CoreBankingRecord record = new CoreBankingRecord();
            record.setCustomerId(customer.getCustomerId());
            record.setApplicationId(app.getApplicationId());
            record.setCustomerName(customer.getFullName());
            record.setAddress(customer.getFullAddress());
            record.setDateOfBirth(customer.getDateOfBirth());
            record.setKycDetails(customer.getKycType() + ": " + customer.getKycReference());
            record.setApprovedAmount(app.getApprovedAmount() != null ? app.getApprovedAmount() : app.getRequestedAmount());
            record.setDisbursedAmount(disbursement.getAmount());
            record.setDisbursedDate(disbursement.getDisbursedAt());
            record.setPushedAt(now);

            coreBankingDAO.save(record);

            // 2. Move application to EXIT stage
            loanApplicationDAO.updateStatus(app.getApplicationId(), ApplicationStatus.EXIT);

            // 3. Save application history
            ApplicationHistory history = new ApplicationHistory(
                    null,
                    app.getApplicationId(),
                    "CORE_PUSH",
                    ApplicationStatus.DISBURSED.name(),
                    ApplicationStatus.EXIT.name(),
                    "CORE_DATA_PUSH_JOB",
                    now,
                    "Disbursement details successfully synced to NG_CORE_BANKING. Case marked EXIT."
            );
            historyDAO.save(history);

            log.info("Application {} successfully pushed to NG_CORE_BANKING and transitioned to EXIT.",
                    app.getApplicationNumber());
            return true;
        }));
    }

    public static class CorePushExecutionSummary {
        private final Long runId;
        private final UtilityResult result;
        private final int processedCount;
        private final int successfulCount;
        private final String errorMessage;

        public CorePushExecutionSummary(Long runId, UtilityResult result, int processedCount,
                                        int successfulCount, String errorMessage) {
            this.runId = runId;
            this.result = result;
            this.processedCount = processedCount;
            this.successfulCount = successfulCount;
            this.errorMessage = errorMessage;
        }

        public Long getRunId() { return runId; }
        public UtilityResult getResult() { return result; }
        public int getProcessedCount() { return processedCount; }
        public int getSuccessfulCount() { return successfulCount; }
        public String getErrorMessage() { return errorMessage; }
    }
}
