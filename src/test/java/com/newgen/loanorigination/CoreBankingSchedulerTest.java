package com.newgen.loanorigination;

import com.newgen.loanorigination.config.BusinessClock;
import com.newgen.loanorigination.dao.*;
import com.newgen.loanorigination.model.Customer;
import com.newgen.loanorigination.model.Disbursement;
import com.newgen.loanorigination.model.LoanApplication;
import com.newgen.loanorigination.model.enums.ApplicationStatus;
import com.newgen.loanorigination.model.enums.ProductType;
import com.newgen.loanorigination.model.enums.UtilityResult;
import com.newgen.loanorigination.scheduler.CoreBankingScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("8. CoreDataPush Utility & Repeat-Run Safety Tests")
class CoreBankingSchedulerTest {

    private LoanApplicationDAO loanApplicationDAO;
    private CustomerDAO customerDAO;
    private DisbursementDAO disbursementDAO;
    private CoreBankingDAO coreBankingDAO;
    private ApplicationHistoryDAO historyDAO;
    private UtilityRunDAO utilityRunDAO;
    private CoreBankingScheduler coreBankingScheduler;

    @BeforeEach
    void setUp() {
        loanApplicationDAO = mock(LoanApplicationDAO.class);
        customerDAO = mock(CustomerDAO.class);
        disbursementDAO = mock(DisbursementDAO.class);
        coreBankingDAO = mock(CoreBankingDAO.class);
        historyDAO = mock(ApplicationHistoryDAO.class);
        utilityRunDAO = mock(UtilityRunDAO.class);

        PlatformTransactionManager txManager = new org.springframework.transaction.support.AbstractPlatformTransactionManager() {
            @Override
            protected Object doGetTransaction() {
                return new Object();
            }

            @Override
            protected void doBegin(Object transaction, org.springframework.transaction.TransactionDefinition definition) {
            }

            @Override
            protected void doCommit(org.springframework.transaction.support.DefaultTransactionStatus status) {
            }

            @Override
            protected void doRollback(org.springframework.transaction.support.DefaultTransactionStatus status) {
            }
        };

        BusinessClock clock = new BusinessClock("2026-11-23");

        coreBankingScheduler = new CoreBankingScheduler(
                loanApplicationDAO,
                customerDAO,
                disbursementDAO,
                coreBankingDAO,
                historyDAO,
                utilityRunDAO,
                txManager,
                clock
        );

        when(utilityRunDAO.startRun(any(), any())).thenReturn(1L);
    }

    private LoanApplication createDisbursedApp(Long id) {
        LoanApplication app = new LoanApplication();
        app.setApplicationId(id);
        app.setApplicationNumber("APP-202611-" + id);
        app.setCustomerId(1L);
        app.setProductType(ProductType.PERSONAL);
        app.setRequestedAmount(new BigDecimal("100000.00"));
        app.setApprovedAmount(new BigDecimal("100000.00"));
        app.setStatus(ApplicationStatus.DISBURSED);
        return app;
    }

    @Test
    @DisplayName("Scenario 30: Successful core banking push advances status to EXIT")
    void testSuccessfulPushAdvancesToExit() {
        LoanApplication app = createDisbursedApp(1L);
        when(loanApplicationDAO.findDisbursedPendingPush()).thenReturn(List.of(app));
        when(coreBankingDAO.existsByApplicationId(1L)).thenReturn(false);

        Customer customer = new Customer();
        customer.setCustomerId(1L);
        customer.setFullName("Aarav Sharma");
        customer.setDateOfBirth(LocalDate.of(1995, 5, 15));
        customer.setKycType("AADHAAR");
        customer.setKycReference("1234-5678");
        customer.setAddressLine("Sector 62");
        when(customerDAO.findById(1L)).thenReturn(Optional.of(customer));

        Disbursement disbursement = new Disbursement();
        disbursement.setDisbursementId(10L);
        disbursement.setApplicationId(1L);
        disbursement.setAmount(new BigDecimal("100000.00"));
        disbursement.setDisbursedAt(LocalDateTime.now());
        when(disbursementDAO.findByApplicationId(1L)).thenReturn(Optional.of(disbursement));

        CoreBankingScheduler.CorePushExecutionSummary summary = coreBankingScheduler.runCoreDataPushNow();

        assertEquals(UtilityResult.SUCCESS, summary.getResult());
        assertEquals(1, summary.getProcessedCount());
        assertEquals(1, summary.getSuccessfulCount());

        verify(coreBankingDAO, times(1)).save(any());
        verify(loanApplicationDAO, times(1)).updateStatus(1L, ApplicationStatus.EXIT);
        verify(historyDAO, times(1)).save(any());
    }

    @Test
    @DisplayName("Scenario 29: Repeated core banking push generates NO duplicate rows in NG_CORE_BANKING")
    void testRepeatedPushNoDuplicateRows() {
        LoanApplication app = createDisbursedApp(1L);
        when(loanApplicationDAO.findDisbursedPendingPush()).thenReturn(List.of(app));

        // Already exists in NG_CORE_BANKING
        when(coreBankingDAO.existsByApplicationId(1L)).thenReturn(true);

        CoreBankingScheduler.CorePushExecutionSummary summary = coreBankingScheduler.runCoreDataPushNow();

        assertEquals(UtilityResult.SUCCESS, summary.getResult());
        // save() was NEVER called because duplicate check detected existing record!
        verify(coreBankingDAO, never()).save(any());
        verify(loanApplicationDAO, times(1)).updateStatus(1L, ApplicationStatus.EXIT);
    }
}
