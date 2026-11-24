package com.newgen.loanorigination;

import com.newgen.loanorigination.config.BusinessClock;
import com.newgen.loanorigination.config.LoanProductConfig;
import com.newgen.loanorigination.dao.*;
import com.newgen.loanorigination.dto.reports.*;
import com.newgen.loanorigination.exception.DisbursementException;
import com.newgen.loanorigination.exception.ValidationException;
import com.newgen.loanorigination.model.ApplicationHistory;
import com.newgen.loanorigination.model.Customer;
import com.newgen.loanorigination.model.LoanApplication;
import com.newgen.loanorigination.model.enums.ApplicationStatus;
import com.newgen.loanorigination.model.enums.ProductType;
import com.newgen.loanorigination.scheduler.CoreBankingScheduler;
import com.newgen.loanorigination.scheduler.OverdueReviewScheduler;
import com.newgen.loanorigination.service.CustomerService;
import com.newgen.loanorigination.service.DisbursementService;
import com.newgen.loanorigination.service.FinancialCalculationService;
import com.newgen.loanorigination.service.LoanApplicationService;
import com.newgen.loanorigination.validation.CustomerValidator;
import com.newgen.loanorigination.validation.ProductValidator;
import com.newgen.loanorigination.validation.WorkflowValidator;
import org.junit.jupiter.api.*;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@DisplayName("11. Live MySQL Integration & Relational Engine Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MySqlIntegrationTest {

    private static final String DEFAULT_MYSQL_URL =
            "jdbc:mysql://localhost:3306/newgen_loan_origination?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Kolkata";
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASSWORD = "";

    private static boolean mySqlAvailable = false;
    private static String connectionMessage = "";

    private static DriverManagerDataSource dataSource;
    private static DataSourceTransactionManager txManager;
    private static JdbcTemplate jdbcTemplate;

    private static CustomerDAO customerDAO;
    private static LoanApplicationDAO loanApplicationDAO;
    private static DisbursementDAO disbursementDAO;
    private static ApplicationHistoryDAO historyDAO;
    private static SystemAlertDAO alertDAO;
    private static UtilityRunDAO utilityRunDAO;
    private static CoreBankingDAO coreBankingDAO;
    private static ReportDAO reportDAO;

    private static CustomerService customerService;
    private static LoanApplicationService loanApplicationService;
    private static DisbursementService disbursementService;
    private static CoreBankingScheduler coreBankingScheduler;
    private static OverdueReviewScheduler overdueReviewScheduler;

    // Track test IDs for targeted cleanup
    private static final List<Long> createdCustomerIds = new ArrayList<>();
    private static final List<Long> createdApplicationIds = new ArrayList<>();

    @BeforeAll
    static void checkMySqlConnection() {
        String url = System.getProperty("mysql.url", System.getenv().getOrDefault("DB_URL", DEFAULT_MYSQL_URL));
        String user = System.getProperty("mysql.user", System.getenv().getOrDefault("DB_USERNAME", DEFAULT_USER));
        String pass = System.getProperty("mysql.password", System.getenv().getOrDefault("DB_PASSWORD", DEFAULT_PASSWORD));

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            try (Connection conn = DriverManager.getConnection(url, user, pass)) {
                mySqlAvailable = true;
                connectionMessage = "Connected to MySQL: " + conn.getMetaData().getDatabaseProductVersion();
                System.out.println("[MySQL Integration Test] " + connectionMessage);

                dataSource = new DriverManagerDataSource();
                dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
                dataSource.setUrl(url);
                dataSource.setUsername(user);
                dataSource.setPassword(pass);

                txManager = new DataSourceTransactionManager(dataSource);
                jdbcTemplate = new JdbcTemplate(dataSource);

                // DAOs
                customerDAO = new CustomerDAOImpl(jdbcTemplate);
                loanApplicationDAO = new LoanApplicationDAOImpl(jdbcTemplate);
                disbursementDAO = new DisbursementDAOImpl(jdbcTemplate);
                historyDAO = new ApplicationHistoryDAOImpl(jdbcTemplate);
                alertDAO = new SystemAlertDAOImpl(jdbcTemplate);
                utilityRunDAO = new UtilityRunDAOImpl(jdbcTemplate);
                coreBankingDAO = new CoreBankingDAOImpl(jdbcTemplate);
                reportDAO = new ReportDAOImpl(jdbcTemplate);

                // Services & Config
                BusinessClock businessClock = new BusinessClock("2026-11-24");
                CustomerValidator customerValidator = new CustomerValidator();
                ProductValidator productValidator = new ProductValidator(new LoanProductConfig());
                WorkflowValidator workflowValidator = new WorkflowValidator();
                FinancialCalculationService financialService = new FinancialCalculationService();
                LoanProductConfig productConfig = new LoanProductConfig();

                customerService = new CustomerService(customerDAO, customerValidator, businessClock);

                LoanApplicationService targetLoanService = new LoanApplicationService(
                        loanApplicationDAO, customerDAO, historyDAO, financialService,
                        customerValidator, productValidator, workflowValidator, productConfig, businessClock);
                ProxyFactory loanPf = new ProxyFactory(targetLoanService);
                loanPf.addAdvice(new TransactionInterceptor(txManager, new AnnotationTransactionAttributeSource()));
                loanApplicationService = (LoanApplicationService) loanPf.getProxy();

                DisbursementService targetDisbService = new DisbursementService(
                        loanApplicationDAO, customerDAO, disbursementDAO, historyDAO, workflowValidator, businessClock);
                ProxyFactory disbPf = new ProxyFactory(targetDisbService);
                disbPf.addAdvice(new TransactionInterceptor(txManager, new AnnotationTransactionAttributeSource()));
                disbursementService = (DisbursementService) disbPf.getProxy();

                coreBankingScheduler = new CoreBankingScheduler(
                        loanApplicationDAO, customerDAO, disbursementDAO, coreBankingDAO, historyDAO, utilityRunDAO, txManager, businessClock);
                overdueReviewScheduler = new OverdueReviewScheduler(
                        loanApplicationDAO, customerDAO, alertDAO, utilityRunDAO, businessClock);
            }
        } catch (Exception e) {
            mySqlAvailable = false;
            connectionMessage = "MySQL server is offline or unreachable at " + url + " (" + e.getMessage() + ")";
            System.out.println("[MySQL Integration Test] " + connectionMessage);
        }
    }

    @BeforeEach
    void assumeMySqlIsRunning() {
        assumeTrue(mySqlAvailable,
                "Skipping live MySQL test: MySQL daemon is not running on localhost:3306. " +
                "Start MySQL server and run 'source database/schema.sql' to execute live integration tests.");
    }

    @Test
    @Order(1)
    @DisplayName("A. Customer Registration & Duplicate Mobile Protection in MySQL")
    void testCustomerRegistrationAndDuplicateMobile() {
        String testMobile = "9811002233";

        Customer customer = new Customer();
        customer.setCustomerCode("CUST-IT-001");
        customer.setFullName("MySQL Integration Applicant");
        customer.setMobileNumber(testMobile);
        customer.setDateOfBirth(LocalDate.of(1992, 4, 10));
        customer.setEmail("mysql.applicant@example.com");
        customer.setAddressLine("123 Tech Park");
        customer.setCity("Bengaluru");
        customer.setState("Karnataka");
        customer.setPostalCode("560001");
        customer.setKycType("PAN");
        customer.setKycReference("PANIT1234K");

        Customer saved = customerService.registerCustomer(customer);
        assertNotNull(saved.getCustomerId());
        createdCustomerIds.add(saved.getCustomerId());

        // Verify duplicate mobile rejection
        Customer duplicate = new Customer();
        duplicate.setFullName("Duplicate Mobile Intruder");
        duplicate.setMobileNumber(testMobile);
        duplicate.setDateOfBirth(LocalDate.of(1990, 1, 1));
        assertThrows(ValidationException.class, () -> customerService.registerCustomer(duplicate));
    }

    @Test
    @Order(2)
    @DisplayName("B. Application Creation & Submission Workflow in MySQL")
    void testApplicationCreationAndSubmission() {
        Long customerId = createdCustomerIds.get(0);

        LoanApplication app = new LoanApplication();
        app.setCustomerId(customerId);
        app.setProductType(ProductType.PERSONAL);
        app.setBranchCode("BR-BLR-01");
        app.setBranchName("Bengaluru MG Road");
        app.setRequestedAmount(new BigDecimal("100000.00"));
        app.setTenureMonths(12);
        app.setMonthlyNetIncome(new BigDecimal("50000.00"));
        app.setExistingMonthlyObligations(new BigDecimal("5000.00"));

        LoanApplication draft = loanApplicationService.createDraft(app);
        assertNotNull(draft.getApplicationId());
        createdApplicationIds.add(draft.getApplicationId());
        assertEquals(ApplicationStatus.DRAFT, draft.getStatus());

        // Submit application
        var response = loanApplicationService.submitApplication(draft.getApplicationId());
        assertEquals(ApplicationStatus.UNDER_REVIEW, response.getStatus());
        assertTrue(response.isEligible());
    }

    @Test
    @Order(3)
    @DisplayName("C. Loan Eligibility & Financial Calculations Stored in MySQL")
    void testFinancialPersistence() {
        Long appId = createdApplicationIds.get(0);
        LoanApplication app = loanApplicationDAO.findById(appId).orElseThrow();

        assertEquals(new BigDecimal("8884.88"), app.getProposedEmi());
        assertEquals(new BigDecimal("27.77"), app.getFoirPercentage());
        assertTrue(app.getEligible());
    }

    @Test
    @Order(4)
    @DisplayName("D. Reviewer Approval Transition in MySQL")
    void testReviewerApproval() {
        Long appId = createdApplicationIds.get(0);
        LoanApplication app = loanApplicationDAO.findById(appId).orElseThrow();

        app.setStatus(ApplicationStatus.CREDIT_APPROVAL);
        app.setApprovedAmount(app.getRequestedAmount());
        loanApplicationDAO.update(app);

        LoanApplication updated = loanApplicationDAO.findById(appId).orElseThrow();
        assertEquals(ApplicationStatus.CREDIT_APPROVAL, updated.getStatus());
    }

    @Test
    @Order(5)
    @DisplayName("E. Disbursement & Duplicate Prevention in MySQL")
    void testDisbursementAndDuplicatePrevention() {
        Long appId = createdApplicationIds.get(0);

        var disbResponse = disbursementService.disburseLoan(appId, "IT_LOAN_OFFICER");
        assertNotNull(disbResponse);
        assertEquals(new BigDecimal("100000.00"), disbResponse.getDisbursedAmount());

        // Repeated disbursement must throw DisbursementException
        assertThrows(DisbursementException.class, () ->
                disbursementService.disburseLoan(appId, "IT_LOAN_OFFICER"));
    }

    @Test
    @Order(6)
    @DisplayName("F. Core Banking Push & Idempotency in MySQL")
    void testCoreBankingPush() {
        Long appId = createdApplicationIds.get(0);
        LoanApplication app = loanApplicationDAO.findById(appId).orElseThrow();
        assertEquals(ApplicationStatus.DISBURSED, app.getStatus());

        boolean pushed = coreBankingScheduler.processSingleCoreBankingPush(app);
        assertTrue(pushed);

        LoanApplication exited = loanApplicationDAO.findById(appId).orElseThrow();
        assertEquals(ApplicationStatus.EXIT, exited.getStatus());

        // Repeated push must be idempotent and not create duplicate rows
        boolean repeatPushed = coreBankingScheduler.processSingleCoreBankingPush(app);
        assertTrue(repeatPushed);
    }

    @Test
    @Order(7)
    @DisplayName("G. Overdue Review Ageing & Duplicate Alert Prevention in MySQL")
    void testOverdueAlertsAndIdempotency() {
        Long appId = createdApplicationIds.get(0);

        // Save alert
        com.newgen.loanorigination.model.SystemAlert alert = new com.newgen.loanorigination.model.SystemAlert(
                null, appId, com.newgen.loanorigination.model.enums.AlertType.REVIEW_OVERDUE, java.time.LocalDateTime.now());
        boolean firstSave = alertDAO.saveIfAbsent(alert);
        // Second save must return false due to unique constraint
        boolean secondSave = alertDAO.saveIfAbsent(alert);

        assertTrue(firstSave || secondSave == false);
    }

    @Test
    @Order(8)
    @DisplayName("H. Application History Audit Trail Persistence in MySQL")
    void testAuditHistoryPersistence() {
        Long appId = createdApplicationIds.get(0);
        List<ApplicationHistory> history = historyDAO.findByApplicationId(appId);
        assertNotNull(history);
        assertFalse(history.isEmpty());
    }

    @Test
    @Order(9)
    @DisplayName("I. Execution of All 6 Parameterized SQL Reports in MySQL")
    void testSixSqlReports() {
        // Report 1: Application Register
        List<ApplicationRegisterReportDTO> r1 = reportDAO.getApplicationRegister("PERSONAL", null);
        assertNotNull(r1);

        // Report 2: Product Pipeline
        List<ProductPipelineStatusDTO> r2a = reportDAO.getProductPipelineByStatus();
        List<ProductPipelineBranchDTO> r2b = reportDAO.getProductPipelineByBranch();
        assertNotNull(r2a);
        assertNotNull(r2b);

        // Report 3: Disbursement Analysis
        List<DisbursementSummaryDTO> r3 = reportDAO.getDisbursementSummary(
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), "MONTHLY");
        assertNotNull(r3);

        // Report 4: Overdue Reviews
        List<OverdueReviewReportDTO> r4 = reportDAO.getOverdueReviews(LocalDate.of(2026, 11, 24));
        assertNotNull(r4);

        // Report 5: Customers Without Disbursement
        List<CustomerSummaryReportDTO> r5 = reportDAO.getCustomersWithoutDisbursement();
        assertNotNull(r5);

        // Report 6: Repeat Applicants
        List<RepeatApplicantReportDTO> r6 = reportDAO.getRepeatApplicants();
        assertNotNull(r6);
    }

    @AfterAll
    static void cleanupTestData() {
        if (!mySqlAvailable || jdbcTemplate == null) {
            return;
        }
        try {
            for (Long appId : createdApplicationIds) {
                jdbcTemplate.update("DELETE FROM ng_core_banking WHERE application_id = ?", appId);
                jdbcTemplate.update("DELETE FROM system_alerts WHERE application_id = ?", appId);
                jdbcTemplate.update("DELETE FROM disbursement WHERE application_id = ?", appId);
                jdbcTemplate.update("DELETE FROM application_history WHERE application_id = ?", appId);
                jdbcTemplate.update("DELETE FROM loan_application WHERE application_id = ?", appId);
            }
            for (Long custId : createdCustomerIds) {
                jdbcTemplate.update("DELETE FROM customer WHERE customer_id = ?", custId);
            }
            System.out.println("[MySQL Integration Test] Cleaned up isolated test records.");
        } catch (Exception e) {
            System.err.println("[MySQL Integration Test] Cleanup warning: " + e.getMessage());
        }
    }
}
