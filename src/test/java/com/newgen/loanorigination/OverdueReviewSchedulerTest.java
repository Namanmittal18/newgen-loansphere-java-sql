package com.newgen.loanorigination;

import com.newgen.loanorigination.config.BusinessClock;
import com.newgen.loanorigination.dao.*;
import com.newgen.loanorigination.model.Customer;
import com.newgen.loanorigination.model.LoanApplication;
import com.newgen.loanorigination.model.enums.ApplicationStatus;
import com.newgen.loanorigination.model.enums.ProductType;
import com.newgen.loanorigination.model.enums.UtilityResult;
import com.newgen.loanorigination.scheduler.OverdueReviewScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("7. Overdue Review Utility & Ageing Boundary Tests")
class OverdueReviewSchedulerTest {

    private LoanApplicationDAO loanApplicationDAO;
    private CustomerDAO customerDAO;
    private SystemAlertDAO systemAlertDAO;
    private UtilityRunDAO utilityRunDAO;
    private OverdueReviewScheduler scheduler;

    @TempDir
    File tempExportDir;

    private final LocalDate businessDate = LocalDate.of(2026, 11, 26);

    @BeforeEach
    void setUp() {
        if (tempExportDir == null) {
            tempExportDir = new File(System.getProperty("java.io.tmpdir"), "test-export-" + System.currentTimeMillis());
            tempExportDir.mkdirs();
            tempExportDir.deleteOnExit();
        }

        loanApplicationDAO = mock(LoanApplicationDAO.class);
        customerDAO = mock(CustomerDAO.class);
        systemAlertDAO = mock(SystemAlertDAO.class);
        utilityRunDAO = mock(UtilityRunDAO.class);

        BusinessClock clock = new BusinessClock("2026-11-26");

        scheduler = new OverdueReviewScheduler(
                loanApplicationDAO,
                customerDAO,
                systemAlertDAO,
                utilityRunDAO,
                clock
        );

        ReflectionTestUtils.setField(scheduler, "exportDirectory", tempExportDir.getAbsolutePath());
        ReflectionTestUtils.setField(scheduler, "schedulerEnabled", true);

        when(utilityRunDAO.startRun(any(), any())).thenReturn(1L);
    }

    private LoanApplication createAppWithSubmissionDate(Long id, LocalDate submissionDate) {
        LoanApplication app = new LoanApplication();
        app.setApplicationId(id);
        app.setApplicationNumber("APP-202611-" + id);
        app.setCustomerId(1L);
        app.setProductType(ProductType.PERSONAL);
        app.setRequestedAmount(new BigDecimal("100000.00"));
        app.setStatus(ApplicationStatus.UNDER_REVIEW);
        app.setSubmittedAt(submissionDate.atTime(10, 0));
        return app;
    }

    @Test
    @DisplayName("Scenario 25 & 26: Age exactly 2 days is NOT overdue; age 3 days IS overdue")
    void testOverdueAgeingBoundaries() {
        // Application 1: Submitted on 2026-11-24 (2 calendar days before 2026-11-26) -> NOT overdue!
        LoanApplication app2Days = createAppWithSubmissionDate(1L, LocalDate.of(2026, 11, 24));

        // Application 2: Submitted on 2026-11-23 (3 calendar days before 2026-11-26) -> OVERDUE!
        LoanApplication app3Days = createAppWithSubmissionDate(2L, LocalDate.of(2026, 11, 23));

        when(loanApplicationDAO.findByStatus(ApplicationStatus.UNDER_REVIEW))
                .thenReturn(List.of(app2Days, app3Days));

        Customer mockCustomer = new Customer();
        mockCustomer.setCustomerId(1L);
        mockCustomer.setFullName("Aarav Sharma");
        mockCustomer.setMobileNumber("9876543210");
        when(customerDAO.findById(1L)).thenReturn(Optional.of(mockCustomer));

        when(systemAlertDAO.saveIfAbsent(any())).thenReturn(true);

        OverdueReviewScheduler.UtilityExecutionSummary summary = scheduler.runUtilityNow(businessDate);

        assertEquals(UtilityResult.SUCCESS, summary.getResult());
        assertEquals(2, summary.getProcessedCount());
        // Exactly 1 alert created (for app 2 which is 3 days old)
        assertEquals(1, summary.getAlertsCreated());

        // Verify CSV export file was created
        File exportedFile = new File(summary.getOutputFilePath());
        assertTrue(exportedFile.exists(), "CSV export file must be generated");
        assertTrue(exportedFile.getName().contains("pending_reviews_20261126.csv"));
    }

    @Test
    @DisplayName("Scenario 27: Repeated utility execution produces NO duplicate alerts")
    void testRepeatedUtilityNoDuplicateAlerts() {
        LoanApplication app3Days = createAppWithSubmissionDate(2L, LocalDate.of(2026, 11, 23));
        when(loanApplicationDAO.findByStatus(ApplicationStatus.UNDER_REVIEW))
                .thenReturn(List.of(app3Days));

        Customer mockCustomer = new Customer();
        mockCustomer.setCustomerId(1L);
        mockCustomer.setFullName("Aarav Sharma");
        mockCustomer.setMobileNumber("9876543210");
        when(customerDAO.findById(1L)).thenReturn(Optional.of(mockCustomer));

        // Alert already exists in database (saveIfAbsent returns false)
        when(systemAlertDAO.saveIfAbsent(any())).thenReturn(false);

        OverdueReviewScheduler.UtilityExecutionSummary summary = scheduler.runUtilityNow(businessDate);

        assertEquals(UtilityResult.SUCCESS, summary.getResult());
        assertEquals(1, summary.getProcessedCount());
        // 0 new alerts created because it was already present!
        assertEquals(0, summary.getAlertsCreated());
    }
}
