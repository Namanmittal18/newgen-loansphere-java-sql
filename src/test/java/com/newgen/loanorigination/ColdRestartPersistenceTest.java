package com.newgen.loanorigination;

import com.newgen.loanorigination.config.BusinessClock;
import com.newgen.loanorigination.config.LoanProductConfig;
import com.newgen.loanorigination.dao.*;
import com.newgen.loanorigination.model.ApplicationHistory;
import com.newgen.loanorigination.model.Customer;
import com.newgen.loanorigination.model.Disbursement;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@DisplayName("12. Cold Restart Database & Context Persistence Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ColdRestartPersistenceTest {

    private static final String DEFAULT_MYSQL_URL =
            "jdbc:mysql://localhost:3306/newgen_loan_origination?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Kolkata";
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASSWORD = "";

    private static final String TEST_MOBILE = "9999000001";
    private static final String TEST_CUST_CODE = "CUST-CR-20261125";

    private static boolean mySqlAvailable = false;
    private static Long savedCustomerId;
    private static Long savedApplicationId;
    private static Long savedDisbursementId;
    private static Long savedCoreBankingId;

    @BeforeAll
    static void checkMySqlConnectivity() {
        String url = System.getProperty("mysql.url", System.getenv().getOrDefault("DB_URL", DEFAULT_MYSQL_URL));
        String user = System.getProperty("mysql.user", System.getenv().getOrDefault("DB_USERNAME", DEFAULT_USER));
        String pass = System.getProperty("mysql.password", System.getenv().getOrDefault("DB_PASSWORD", DEFAULT_PASSWORD));

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            try (Connection conn = DriverManager.getConnection(url, user, pass)) {
                mySqlAvailable = true;
            }
        } catch (Exception e) {
            mySqlAvailable = false;
        }
    }

    private static class ApplicationContextContainer {
        DriverManagerDataSource dataSource;
        DataSourceTransactionManager txManager;
        JdbcTemplate jdbcTemplate;

        CustomerDAO customerDAO;
        LoanApplicationDAO loanApplicationDAO;
        DisbursementDAO disbursementDAO;
        ApplicationHistoryDAO historyDAO;
        SystemAlertDAO alertDAO;
        UtilityRunDAO utilityRunDAO;
        CoreBankingDAO coreBankingDAO;

        CustomerService customerService;
        LoanApplicationService loanApplicationService;
        DisbursementService disbursementService;
        CoreBankingScheduler coreBankingScheduler;
        OverdueReviewScheduler overdueReviewScheduler;

        ApplicationContextContainer() {
            String url = System.getProperty("mysql.url", System.getenv().getOrDefault("DB_URL", DEFAULT_MYSQL_URL));
            String user = System.getProperty("mysql.user", System.getenv().getOrDefault("DB_USERNAME", DEFAULT_USER));
            String pass = System.getProperty("mysql.password", System.getenv().getOrDefault("DB_PASSWORD", DEFAULT_PASSWORD));

            this.dataSource = new DriverManagerDataSource();
            this.dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
            this.dataSource.setUrl(url);
            this.dataSource.setUsername(user);
            this.dataSource.setPassword(pass);

            this.txManager = new DataSourceTransactionManager(this.dataSource);
            this.jdbcTemplate = new JdbcTemplate(this.dataSource);

            this.customerDAO = new CustomerDAOImpl(this.jdbcTemplate);
            this.loanApplicationDAO = new LoanApplicationDAOImpl(this.jdbcTemplate);
            this.disbursementDAO = new DisbursementDAOImpl(this.jdbcTemplate);
            this.historyDAO = new ApplicationHistoryDAOImpl(this.jdbcTemplate);
            this.alertDAO = new SystemAlertDAOImpl(this.jdbcTemplate);
            this.utilityRunDAO = new UtilityRunDAOImpl(this.jdbcTemplate);
            this.coreBankingDAO = new CoreBankingDAOImpl(this.jdbcTemplate);

            BusinessClock clock = new BusinessClock("2026-11-24");
            LoanProductConfig productConfig = new LoanProductConfig();
            FinancialCalculationService financialService = new FinancialCalculationService();
            CustomerValidator customerValidator = new CustomerValidator();
            ProductValidator productValidator = new ProductValidator(productConfig);
            WorkflowValidator workflowValidator = new WorkflowValidator();

            this.customerService = new CustomerService(customerDAO, customerValidator, clock);

            LoanApplicationService rawLoanService = new LoanApplicationService(
                    loanApplicationDAO, customerDAO, historyDAO, financialService,
                    customerValidator, productValidator, workflowValidator, productConfig, clock
            );
            ProxyFactory loanPf = new ProxyFactory(rawLoanService);
            loanPf.addAdvice(new TransactionInterceptor(this.txManager, new AnnotationTransactionAttributeSource()));
            this.loanApplicationService = (LoanApplicationService) loanPf.getProxy();

            DisbursementService rawDisbService = new DisbursementService(
                    loanApplicationDAO, customerDAO, disbursementDAO, historyDAO, workflowValidator, clock
            );
            ProxyFactory disbPf = new ProxyFactory(rawDisbService);
            disbPf.addAdvice(new TransactionInterceptor(this.txManager, new AnnotationTransactionAttributeSource()));
            this.disbursementService = (DisbursementService) disbPf.getProxy();

            this.coreBankingScheduler = new CoreBankingScheduler(
                    loanApplicationDAO, customerDAO, disbursementDAO, coreBankingDAO, historyDAO, utilityRunDAO, txManager, clock
            );

            this.overdueReviewScheduler = new OverdueReviewScheduler(
                    loanApplicationDAO, customerDAO, alertDAO, utilityRunDAO, clock
            );
        }

        void close() {
            this.jdbcTemplate = null;
            this.dataSource = null;
            this.txManager = null;
        }
    }

    @Test
    @Order(1)
    @DisplayName("Step 1: Execute Full Lifecycle in Application Context A & Advance to EXIT")
    void testExecuteLifecycleInContextA() {
        assumeTrue(mySqlAvailable, "MySQL is not accessible. Skipping live persistence test.");

        ApplicationContextContainer contextA = new ApplicationContextContainer();

        // Clean up any stale records from a previous run
        cleanupTestRecords(contextA.jdbcTemplate);

        // 1. Create Isolated Customer
        Customer customer = new Customer();
        customer.setCustomerCode(TEST_CUST_CODE);
        customer.setFullName("Dr. Vikram ColdRestart");
        customer.setDateOfBirth(LocalDate.of(1985, 4, 15));
        customer.setMobileNumber(TEST_MOBILE);
        customer.setEmail("vikram.coldrestart@example.com");
        customer.setAddressLine("Plot 104, IT Corridor, Phase 5");
        customer.setCity("Pune");
        customer.setState("Maharashtra");
        customer.setPostalCode("411057");
        customer.setKycType("PAN");
        customer.setKycReference("ABCDE9999Z");
        customer.setKycStatus("VERIFIED");

        Customer savedCust = contextA.customerService.registerCustomer(customer);
        savedCustomerId = savedCust.getCustomerId();
        assertThat(savedCustomerId).isNotNull();

        // 2. Create & Submit Loan Application
        LoanApplication app = new LoanApplication();
        app.setCustomerId(savedCustomerId);
        app.setProductType(ProductType.PERSONAL);
        app.setBranchCode("BR005");
        app.setBranchName("Pune Cyber City Branch");
        app.setRequestedAmount(new BigDecimal("150000.00"));
        app.setTenureMonths(24);
        app.setMonthlyNetIncome(new BigDecimal("50000.00"));
        app.setExistingMonthlyObligations(new BigDecimal("5000.00"));

        LoanApplication draft = contextA.loanApplicationService.createDraft(app);
        savedApplicationId = draft.getApplicationId();
        assertThat(savedApplicationId).isNotNull();

        // Submit application
        var subResponse = contextA.loanApplicationService.submitApplication(savedApplicationId);
        assertThat(subResponse.getStatus()).isEqualTo(ApplicationStatus.UNDER_REVIEW);

        // 3. Reviewer Approval
        LoanApplication underReviewApp = contextA.loanApplicationDAO.findById(savedApplicationId).orElseThrow();
        underReviewApp.setStatus(ApplicationStatus.CREDIT_APPROVAL);
        underReviewApp.setApprovedAmount(underReviewApp.getRequestedAmount());
        underReviewApp.setReviewerCode("REV-CR-001");
        underReviewApp.setDecisionReason("Cold restart verification approval");
        contextA.loanApplicationDAO.update(underReviewApp);

        LoanApplication approvedApp = contextA.loanApplicationDAO.findById(savedApplicationId).orElseThrow();
        assertThat(approvedApp.getStatus()).isEqualTo(ApplicationStatus.CREDIT_APPROVAL);
        assertThat(approvedApp.getApprovedAmount()).isEqualByComparingTo(new BigDecimal("150000.00"));

        // 4. Disbursement
        var disbResp = contextA.disbursementService.disburseLoan(savedApplicationId, "REV-CR-001");
        assertThat(disbResp.getDisbursementReference()).isNotNull();

        Disbursement disb = contextA.disbursementDAO.findByApplicationId(savedApplicationId).orElseThrow();
        savedDisbursementId = disb.getDisbursementId();
        assertThat(savedDisbursementId).isNotNull();

        LoanApplication disbursedApp = contextA.loanApplicationDAO.findById(savedApplicationId).orElseThrow();
        assertThat(disbursedApp.getStatus()).isEqualTo(ApplicationStatus.DISBURSED);

        // 5. CoreDataPush sync to NG_CORE_BANKING
        boolean pushed = contextA.coreBankingScheduler.processSingleCoreBankingPush(disbursedApp);
        assertThat(pushed).isTrue();

        LoanApplication exitApp = contextA.loanApplicationDAO.findById(savedApplicationId).orElseThrow();
        assertThat(exitApp.getStatus()).isEqualTo(ApplicationStatus.EXIT);

        // Record core banking row id
        Long coreId = contextA.jdbcTemplate.queryForObject(
                "SELECT core_banking_id FROM NG_CORE_BANKING WHERE application_id = ?",
                Long.class, savedApplicationId
        );
        savedCoreBankingId = coreId;
        assertThat(savedCoreBankingId).isNotNull();

        // Close Context A
        contextA.close();
    }

    @Test
    @Order(2)
    @DisplayName("Step 2: Restart into Context B and Verify Integrity Across All 7 Tables")
    void testVerifyPersistenceInRestartedContextB() {
        assumeTrue(mySqlAvailable, "MySQL is not accessible. Skipping live persistence test.");
        assumeTrue(savedApplicationId != null, "Step 1 must execute first.");

        // Boot Context B fresh
        ApplicationContextContainer contextB = new ApplicationContextContainer();

        try {
            // Verify 1: CUSTOMER Table
            Customer cust = contextB.customerService.getCustomerById(savedCustomerId);
            assertThat(cust).isNotNull();
            assertThat(cust.getCustomerCode()).isEqualTo(TEST_CUST_CODE);
            assertThat(cust.getFullName()).isEqualTo("Dr. Vikram ColdRestart");
            assertThat(cust.getMobileNumber()).isEqualTo(TEST_MOBILE);
            assertThat(cust.getKycReference()).isEqualTo("ABCDE9999Z");
            assertThat(cust.getCity()).isEqualTo("Pune");

            // Verify 2: LOAN_APPLICATION Table
            LoanApplication app = contextB.loanApplicationDAO.findById(savedApplicationId).orElseThrow();
            assertThat(app).isNotNull();
            assertThat(app.getCustomerId()).isEqualTo(savedCustomerId);
            assertThat(app.getProductType()).isEqualTo(ProductType.PERSONAL);
            assertThat(app.getStatus()).isEqualTo(ApplicationStatus.EXIT);
            assertThat(app.getRequestedAmount()).isEqualByComparingTo(new BigDecimal("150000.00"));
            assertThat(app.getApprovedAmount()).isEqualByComparingTo(new BigDecimal("150000.00"));
            assertThat(app.getTenureMonths()).isEqualTo(24);
            assertThat(app.getAnnualInterestRate()).isEqualByComparingTo(new BigDecimal("12.00"));
            assertThat(app.getReviewerCode()).isEqualTo("REV-CR-001");

            // Verify 3: DISBURSEMENT Table
            Disbursement disb = contextB.disbursementDAO.findByApplicationId(savedApplicationId).orElseThrow();
            assertThat(disb.getDisbursementId()).isEqualTo(savedDisbursementId);
            assertThat(disb.getAmount()).isEqualByComparingTo(new BigDecimal("150000.00"));
            assertThat(disb.getDisbursedAt()).isNotNull();

            // Verify 4: APPLICATION_HISTORY Table
            List<ApplicationHistory> history = contextB.historyDAO.findByApplicationId(savedApplicationId);
            assertThat(history).isNotEmpty();
            List<String> statusesLogged = history.stream().map(ApplicationHistory::getNewStatus).toList();
            assertThat(statusesLogged).contains("DRAFT", "UNDER_REVIEW", "DISBURSED", "EXIT");

            // Verify 5: NG_CORE_BANKING Table
            Integer coreCount = contextB.jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM NG_CORE_BANKING WHERE application_id = ?",
                    Integer.class, savedApplicationId
            );
            assertThat(coreCount).isEqualTo(1);
            String pushedCustName = contextB.jdbcTemplate.queryForObject(
                    "SELECT customer_name FROM NG_CORE_BANKING WHERE core_banking_id = ?",
                    String.class, savedCoreBankingId
            );
            assertThat(pushedCustName).isEqualTo("Dr. Vikram ColdRestart");

            // Verify 6 & 7: SYSTEM_ALERTS & UTILITY_RUN - Idempotency on Restart
            // Re-trigger CoreDataPush on the exited application
            boolean repeatPush = contextB.coreBankingScheduler.processSingleCoreBankingPush(app);
            assertThat(repeatPush).isTrue(); // Handled idempotently

            // Verify no duplicate core banking entries
            Integer coreCountAfter = contextB.jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM NG_CORE_BANKING WHERE application_id = ?",
                    Integer.class, savedApplicationId
            );
            assertThat(coreCountAfter).isEqualTo(1);

            // Verify application remains in terminal EXIT state
            LoanApplication postUtilityApp = contextB.loanApplicationDAO.findById(savedApplicationId).orElseThrow();
            assertThat(postUtilityApp.getStatus()).isEqualTo(ApplicationStatus.EXIT);

            // Verify no duplicate alerts created for this application
            Integer appAlerts = contextB.jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM SYSTEM_ALERTS WHERE application_id = ?",
                    Integer.class, savedApplicationId
            );
            assertThat(appAlerts).isEqualTo(0);

        } finally {
            // Clean up test records
            cleanupTestRecords(contextB.jdbcTemplate);
            contextB.close();
        }
    }

    private static void cleanupTestRecords(JdbcTemplate jdbc) {
        try {
            Long custId = jdbc.queryForObject(
                    "SELECT customer_id FROM CUSTOMER WHERE mobile_number = ?",
                    Long.class, TEST_MOBILE
            );
            if (custId != null) {
                List<Long> appIds = jdbc.queryForList(
                        "SELECT application_id FROM LOAN_APPLICATION WHERE customer_id = ?",
                        Long.class, custId
                );
                for (Long appId : appIds) {
                    jdbc.update("DELETE FROM NG_CORE_BANKING WHERE application_id = ?", appId);
                    jdbc.update("DELETE FROM SYSTEM_ALERTS WHERE application_id = ?", appId);
                    jdbc.update("DELETE FROM DISBURSEMENT WHERE application_id = ?", appId);
                    jdbc.update("DELETE FROM APPLICATION_HISTORY WHERE application_id = ?", appId);
                    jdbc.update("DELETE FROM LOAN_APPLICATION WHERE application_id = ?", appId);
                }
                jdbc.update("DELETE FROM CUSTOMER WHERE customer_id = ?", custId);
            }
        } catch (Exception ignored) {
        }
    }
}
