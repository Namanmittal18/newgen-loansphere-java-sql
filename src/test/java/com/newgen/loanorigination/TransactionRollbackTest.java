package com.newgen.loanorigination;

import com.newgen.loanorigination.config.BusinessClock;
import com.newgen.loanorigination.config.LoanProductConfig;
import com.newgen.loanorigination.dao.*;
import com.newgen.loanorigination.model.ApplicationHistory;
import com.newgen.loanorigination.model.Customer;
import com.newgen.loanorigination.model.LoanApplication;
import com.newgen.loanorigination.model.enums.ApplicationStatus;
import com.newgen.loanorigination.model.enums.ProductType;
import com.newgen.loanorigination.service.DisbursementService;
import com.newgen.loanorigination.service.FinancialCalculationService;
import com.newgen.loanorigination.service.LoanApplicationService;
import com.newgen.loanorigination.validation.CustomerValidator;
import com.newgen.loanorigination.validation.ProductValidator;
import com.newgen.loanorigination.validation.WorkflowValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("10. Multi-Table Transaction Rollback & Database Atomicity Integration Tests")
public class TransactionRollbackTest {

    private JdbcTemplate jdbcTemplate;
    private DataSourceTransactionManager txManager;

    private CustomerDAOImpl customerDAO;
    private LoanApplicationDAOImpl loanApplicationDAO;
    private DisbursementDAOImpl disbursementDAO;
    private ControlledFailureHistoryDAO historyDAO;

    private LoanApplicationService transactionalLoanService;
    private DisbursementService transactionalDisbursementService;

    private Customer testCustomer;

    /**
     * Test-specific hook subclass of ApplicationHistoryDAOImpl.
     * Allows introducing a controlled failure during multi-table transactional workflows
     * without modifying production business logic.
     */
    static class ControlledFailureHistoryDAO extends ApplicationHistoryDAOImpl {
        private boolean failOnSave = false;

        public ControlledFailureHistoryDAO(JdbcTemplate jdbcTemplate) {
            super(jdbcTemplate);
        }

        public void setFailOnSave(boolean failOnSave) {
            this.failOnSave = failOnSave;
        }

        @Override
        public void save(ApplicationHistory history) {
            if (failOnSave) {
                throw new RuntimeException("Simulated intentional database failure during audit history logging.");
            }
            super.save(history);
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:rollback_testdb;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        dataSource.setPassword("");

        txManager = new DataSourceTransactionManager(dataSource);
        jdbcTemplate = new JdbcTemplate(dataSource);

        // Initialize H2 schema from database/schema.sql
        String schema = Files.readString(Paths.get("database/schema.sql"));
        schema = schema.replaceAll("(?i)CREATE DATABASE[^;]+;", "").replaceAll("(?i)USE [^;]+;", "");
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            for (String sql : schema.split(";")) {
                if (!sql.trim().isEmpty()) {
                    stmt.execute(sql.trim());
                }
            }
        }

        // Clean tables
        jdbcTemplate.execute("DELETE FROM ng_core_banking");
        jdbcTemplate.execute("DELETE FROM system_alerts");
        jdbcTemplate.execute("DELETE FROM application_history");
        jdbcTemplate.execute("DELETE FROM disbursement");
        jdbcTemplate.execute("DELETE FROM utility_run");
        jdbcTemplate.execute("DELETE FROM loan_application");
        jdbcTemplate.execute("DELETE FROM customer");

        // Instantiate DAOs with H2 JdbcTemplate
        customerDAO = new CustomerDAOImpl(jdbcTemplate);
        loanApplicationDAO = new LoanApplicationDAOImpl(jdbcTemplate);
        disbursementDAO = new DisbursementDAOImpl(jdbcTemplate);
        historyDAO = new ControlledFailureHistoryDAO(jdbcTemplate);

        // Core business dependencies
        FinancialCalculationService financialCalculationService = new FinancialCalculationService();
        CustomerValidator customerValidator = new CustomerValidator();
        ProductValidator productValidator = new ProductValidator(new LoanProductConfig());
        WorkflowValidator workflowValidator = new WorkflowValidator();
        LoanProductConfig productConfig = new LoanProductConfig();
        BusinessClock businessClock = new BusinessClock("2026-11-24");

        // 1. Create target LoanApplicationService and wrap with Spring's TransactionInterceptor
        LoanApplicationService targetLoanService = new LoanApplicationService(
                loanApplicationDAO,
                customerDAO,
                historyDAO,
                financialCalculationService,
                customerValidator,
                productValidator,
                workflowValidator,
                productConfig,
                businessClock
        );
        ProxyFactory loanProxyFactory = new ProxyFactory(targetLoanService);
        loanProxyFactory.addAdvice(new TransactionInterceptor(txManager, new AnnotationTransactionAttributeSource()));
        transactionalLoanService = (LoanApplicationService) loanProxyFactory.getProxy();

        // 2. Create target DisbursementService and wrap with Spring's TransactionInterceptor
        DisbursementService targetDisbService = new DisbursementService(
                loanApplicationDAO,
                customerDAO,
                disbursementDAO,
                historyDAO,
                workflowValidator,
                businessClock
        );
        ProxyFactory disbProxyFactory = new ProxyFactory(targetDisbService);
        disbProxyFactory.addAdvice(new TransactionInterceptor(txManager, new AnnotationTransactionAttributeSource()));
        transactionalDisbursementService = (DisbursementService) disbProxyFactory.getProxy();

        // Create baseline test customer
        Customer cust = new Customer();
        cust.setCustomerCode("CUST-20261124-0001");
        cust.setFullName("Aarav Sharma");
        cust.setMobileNumber("9876543210");
        cust.setDateOfBirth(LocalDate.of(1995, 5, 15));
        cust.setEmail("aarav.sharma@example.com");
        cust.setAddressLine("Flat 402, Lotus Heights");
        cust.setCity("Noida");
        cust.setState("Uttar Pradesh");
        cust.setPostalCode("201309");
        cust.setKycType("AADHAAR");
        cust.setKycReference("1234-5678-9012");
        testCustomer = customerDAO.save(cust);
    }

    @Test
    @DisplayName("Test A: Application Creation/Submission Rollback – multi-table insert rolls back on failure")
    void testApplicationCreationRollback() {
        // Step 1: Verify database starts with 0 applications and 0 history records
        int initialAppCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM loan_application", Integer.class);
        int initialHistCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM application_history", Integer.class);
        assertEquals(0, initialAppCount, "Initial application row count must be 0.");
        assertEquals(0, initialHistCount, "Initial history row count must be 0.");

        // Step 2 & 3: Prepare valid application to create and save
        LoanApplication newApp = new LoanApplication();
        newApp.setCustomerId(testCustomer.getCustomerId());
        newApp.setProductType(ProductType.PERSONAL);
        newApp.setBranchCode("BR-DEL-01");
        newApp.setBranchName("New Delhi Connaught Place");
        newApp.setRequestedAmount(new BigDecimal("100000.00"));
        newApp.setTenureMonths(12);
        newApp.setMonthlyNetIncome(new BigDecimal("50000.00"));
        newApp.setExistingMonthlyObligations(new BigDecimal("5000.00"));

        // Step 4 & 5: Introduce controlled test hook failure during history record logging
        historyDAO.setFailOnSave(true);

        // Execute transactional method and verify exception is thrown
        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                transactionalLoanService.createDraft(newApp));
        assertTrue(ex.getMessage().contains("Simulated intentional database failure"));

        // Step 6 & 7: Verify via direct SQL query that both application and history records were ROLLED BACK
        int finalAppCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM loan_application", Integer.class);
        int finalHistCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM application_history", Integer.class);

        System.out.println("Test A Verification (Application Creation Rollback):");
        System.out.println("  Before Attempt -> Applications: " + initialAppCount + ", History: " + initialHistCount);
        System.out.println("  After Failure  -> Applications: " + finalAppCount + ", History: " + finalHistCount);

        assertEquals(0, finalAppCount,
                "ATOMICITY VIOLATION: Loan application record was not rolled back after transaction failure!");
        assertEquals(0, finalHistCount,
                "ATOMICITY VIOLATION: Application history record was not rolled back after transaction failure!");
    }

    @Test
    @DisplayName("Test B: Disbursement Rollback – inserted disbursement & status update roll back on failure")
    void testDisbursementRollback() {
        // Step 1: Create a valid CREDIT_APPROVAL application
        LoanApplication app = new LoanApplication();
        app.setApplicationNumber("APP-20261124-0001");
        app.setCustomerId(testCustomer.getCustomerId());
        app.setProductType(ProductType.PERSONAL);
        app.setBranchCode("BR-DEL-01");
        app.setBranchName("New Delhi Connaught Place");
        app.setRequestedAmount(new BigDecimal("100000.00"));
        app.setApprovedAmount(new BigDecimal("100000.00"));
        app.setTenureMonths(12);
        app.setMonthlyNetIncome(new BigDecimal("50000.00"));
        app.setExistingMonthlyObligations(new BigDecimal("5000.00"));
        app.setAnnualInterestRate(new BigDecimal("12.00"));
        app.setProposedEmi(new BigDecimal("8884.88"));
        app.setFoirPercentage(new BigDecimal("27.77"));
        app.setEligible(true);
        app.setStatus(ApplicationStatus.CREDIT_APPROVAL);
        LoanApplication savedApp = loanApplicationDAO.save(app);

        Long appId = savedApp.getApplicationId();

        // Step 2: Record baseline state before disbursement
        int beforeDisbCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM disbursement WHERE application_id = " + appId, Integer.class);
        String beforeStatus = jdbcTemplate.queryForObject("SELECT status FROM loan_application WHERE application_id = " + appId, String.class);
        int beforeHistCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM application_history WHERE application_id = " + appId, Integer.class);

        assertEquals(0, beforeDisbCount);
        assertEquals(ApplicationStatus.CREDIT_APPROVAL.name(), beforeStatus);

        // Step 3 & 4: Begin disbursement operation with controlled failure at history logging
        historyDAO.setFailOnSave(true);

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                transactionalDisbursementService.disburseLoan(appId, "LOAN_OFFICER_01"));
        assertTrue(ex.getMessage().contains("Simulated intentional database failure"));

        // Step 5, 6 & 7: Verify final persisted state via direct SQL queries
        int afterDisbCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM disbursement WHERE application_id = " + appId, Integer.class);
        String afterStatus = jdbcTemplate.queryForObject("SELECT status FROM loan_application WHERE application_id = " + appId, String.class);
        int afterHistCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM application_history WHERE application_id = " + appId, Integer.class);

        System.out.println("Test B Verification (Disbursement Rollback):");
        System.out.println("  Before Attempt -> Disbursements: " + beforeDisbCount + ", Status: " + beforeStatus + ", History: " + beforeHistCount);
        System.out.println("  After Failure  -> Disbursements: " + afterDisbCount + ", Status: " + afterStatus + ", History: " + afterHistCount);

        // 1. Disbursement record MUST be rolled back
        assertEquals(0, afterDisbCount,
                "ATOMICITY VIOLATION: Inserted disbursement row was not rolled back!");

        // 2. Loan application status MUST remain CREDIT_APPROVAL (update rolled back)
        assertEquals(ApplicationStatus.CREDIT_APPROVAL.name(), afterStatus,
                "ATOMICITY VIOLATION: Application status update was not rolled back to CREDIT_APPROVAL!");

        // 3. No partial audit history record MUST remain
        assertEquals(0, afterHistCount,
                "ATOMICITY VIOLATION: Partial history record was committed!");
    }

    @Test
    @DisplayName("Test C: Successful Transaction Control – all multi-table records commit consistently")
    void testSuccessfulTransactionCommit() {
        // Step 1: Create a valid CREDIT_APPROVAL application
        LoanApplication app = new LoanApplication();
        app.setApplicationNumber("APP-20261124-0002");
        app.setCustomerId(testCustomer.getCustomerId());
        app.setProductType(ProductType.PERSONAL);
        app.setBranchCode("BR-DEL-01");
        app.setBranchName("New Delhi Connaught Place");
        app.setRequestedAmount(new BigDecimal("150000.00"));
        app.setApprovedAmount(new BigDecimal("150000.00"));
        app.setTenureMonths(18);
        app.setMonthlyNetIncome(new BigDecimal("60000.00"));
        app.setExistingMonthlyObligations(new BigDecimal("5000.00"));
        app.setAnnualInterestRate(new BigDecimal("12.00"));
        app.setProposedEmi(new BigDecimal("9142.15"));
        app.setFoirPercentage(new BigDecimal("23.57"));
        app.setEligible(true);
        app.setStatus(ApplicationStatus.CREDIT_APPROVAL);
        LoanApplication savedApp = loanApplicationDAO.save(app);

        Long appId = savedApp.getApplicationId();

        // Step 2: Ensure test hook failure is disabled
        historyDAO.setFailOnSave(false);

        // Execute disbursement operation successfully
        assertDoesNotThrow(() ->
                transactionalDisbursementService.disburseLoan(appId, "LOAN_OFFICER_01"));

        // Step 3: Verify all changes are committed and consistent
        int finalDisbCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM disbursement WHERE application_id = " + appId, Integer.class);
        String finalStatus = jdbcTemplate.queryForObject("SELECT status FROM loan_application WHERE application_id = " + appId, String.class);
        int finalHistCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM application_history WHERE application_id = " + appId, Integer.class);

        System.out.println("Test C Verification (Successful Transaction Commit):");
        System.out.println("  Committed State -> Disbursements: " + finalDisbCount + ", Status: " + finalStatus + ", History: " + finalHistCount);

        assertEquals(1, finalDisbCount, "Disbursement record should be committed.");
        assertEquals(ApplicationStatus.DISBURSED.name(), finalStatus, "Application status should be updated to DISBURSED.");
        assertEquals(1, finalHistCount, "Audit history record should be committed.");
    }
}
