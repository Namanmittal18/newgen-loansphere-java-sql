
import com.newgen.loanorigination.config.BusinessClock;
import com.newgen.loanorigination.dao.*;
import com.newgen.loanorigination.model.*;
import com.newgen.loanorigination.model.enums.*;
import com.newgen.loanorigination.service.*;
import com.newgen.loanorigination.validation.*;
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

public class TestPrototype2 {
    public static void main(String[] args) throws Exception {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName("org.h2.Driver");
        ds.setUrl("jdbc:h2:mem:rollback_test2;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        ds.setUsername("sa");
        ds.setPassword("");

        DataSourceTransactionManager txManager = new DataSourceTransactionManager(ds);
        JdbcTemplate jdbcTemplate = new JdbcTemplate(ds);

        // Execute schema
        String schema = Files.readString(Paths.get("database/schema.sql"));
        schema = schema.replaceAll("(?i)CREATE DATABASE[^;]+;", "").replaceAll("(?i)USE [^;]+;", "");
        try (Connection c = ds.getConnection(); Statement s = c.createStatement()) {
            for (String sql : schema.split(";")) {
                if (!sql.trim().isEmpty()) {
                    s.execute(sql.trim());
                }
            }
        }

        // Subclass DAOs for H2 key compatibility if needed, or check how they run
        class TestableCustomerDAO extends CustomerDAOImpl {
            TestableCustomerDAO(JdbcTemplate jt) { super(jt); }
            @Override
            public Customer save(Customer customer) {
                // custom save with explicit column name
                String sql = "INSERT INTO CUSTOMER (customer_code, full_name, date_of_birth, mobile_number, email, address_line, city, state, postal_code, kyc_type, kyc_reference, kyc_status, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)";
                org.springframework.jdbc.support.KeyHolder kh = new org.springframework.jdbc.support.GeneratedKeyHolder();
                jdbcTemplate.update(conn -> {
                    java.sql.PreparedStatement ps = conn.prepareStatement(sql, new String[]{"customer_id"});
                    ps.setString(1, customer.getCustomerCode());
                    ps.setString(2, customer.getFullName());
                    ps.setDate(3, java.sql.Date.valueOf(customer.getDateOfBirth()));
                    ps.setString(4, customer.getMobileNumber());
                    ps.setString(5, customer.getEmail());
                    ps.setString(6, customer.getAddressLine());
                    ps.setString(7, customer.getCity());
                    ps.setString(8, customer.getState());
                    ps.setString(9, customer.getPostalCode());
                    ps.setString(10, customer.getKycType());
                    ps.setString(11, customer.getKycReference());
                    ps.setString(12, customer.getKycStatus() != null ? customer.getKycStatus() : "VERIFIED");
                    return ps;
                }, kh);
                customer.setCustomerId(kh.getKey().longValue());
                return customer;
            }
        }

        class TestableAppDAO extends LoanApplicationDAOImpl {
            TestableAppDAO(JdbcTemplate jt) { super(jt); }
            @Override
            public LoanApplication save(LoanApplication application) {
                String sql = "INSERT INTO LOAN_APPLICATION (application_number, customer_id, product_type, branch_code, " +
                        "branch_name, requested_amount, tenure_months, monthly_net_income, existing_monthly_obligations, " +
                        "annual_interest_rate, proposed_emi, foir_percentage, eligible, eligibility_reasons, status, " +
                        "created_at, submitted_at, decided_at, reviewer_code, decision_reason, approved_amount, approved_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, ?, ?, ?, ?, ?, ?)";
                org.springframework.jdbc.support.KeyHolder kh = new org.springframework.jdbc.support.GeneratedKeyHolder();
                jdbcTemplate.update(conn -> {
                    java.sql.PreparedStatement ps = conn.prepareStatement(sql, new String[]{"application_id"});
                    ps.setString(1, application.getApplicationNumber());
                    ps.setLong(2, application.getCustomerId());
                    ps.setString(3, application.getProductType().name());
                    ps.setString(4, application.getBranchCode());
                    ps.setString(5, application.getBranchName());
                    ps.setBigDecimal(6, application.getRequestedAmount());
                    ps.setInt(7, application.getTenureMonths());
                    ps.setBigDecimal(8, application.getMonthlyNetIncome());
                    ps.setBigDecimal(9, application.getExistingMonthlyObligations());
                    ps.setBigDecimal(10, application.getAnnualInterestRate());
                    ps.setBigDecimal(11, application.getProposedEmi());
                    ps.setBigDecimal(12, application.getFoirPercentage());
                    ps.setBoolean(13, application.getEligible() != null && application.getEligible());
                    ps.setString(14, application.getEligibilityReasons());
                    ps.setString(15, application.getStatus().name());
                    ps.setTimestamp(16, application.getSubmittedAt() != null ? java.sql.Timestamp.valueOf(application.getSubmittedAt()) : null);
                    ps.setTimestamp(17, application.getDecidedAt() != null ? java.sql.Timestamp.valueOf(application.getDecidedAt()) : null);
                    ps.setString(18, application.getReviewerCode());
                    ps.setString(19, application.getDecisionReason());
                    ps.setBigDecimal(20, application.getApprovedAmount());
                    ps.setTimestamp(21, application.getApprovedAt() != null ? java.sql.Timestamp.valueOf(application.getApprovedAt()) : null);
                    return ps;
                }, kh);
                application.setApplicationId(kh.getKey().longValue());
                return application;
            }
        }

        class TestableDisbursementDAO extends DisbursementDAOImpl {
            TestableDisbursementDAO(JdbcTemplate jt) { super(jt); }
            @Override
            public Disbursement save(Disbursement disbursement) {
                String sql = "INSERT INTO DISBURSEMENT (application_id, disbursement_reference, amount, disbursed_at) VALUES (?, ?, ?, CURRENT_TIMESTAMP)";
                org.springframework.jdbc.support.KeyHolder kh = new org.springframework.jdbc.support.GeneratedKeyHolder();
                jdbcTemplate.update(conn -> {
                    java.sql.PreparedStatement ps = conn.prepareStatement(sql, new String[]{"disbursement_id"});
                    ps.setLong(1, disbursement.getApplicationId());
                    ps.setString(2, disbursement.getDisbursementReference());
                    ps.setBigDecimal(3, disbursement.getAmount());
                    return ps;
                }, kh);
                disbursement.setDisbursementId(kh.getKey().longValue());
                return disbursement;
            }
        }

        class TestableHistoryDAO extends ApplicationHistoryDAOImpl {
            boolean fail = false;
            TestableHistoryDAO(JdbcTemplate jt) { super(jt); }
            @Override
            public void save(ApplicationHistory h) {
                if (fail) throw new RuntimeException("Simulated intentional DB failure during audit history logging");
                super.save(h);
            }
        }

        TestableHistoryDAO historyDAO = new TestableHistoryDAO(jdbcTemplate);
        TestableCustomerDAO customerDAO = new TestableCustomerDAO(jdbcTemplate);
        TestableAppDAO appDAO = new TestableAppDAO(jdbcTemplate);
        TestableDisbursementDAO disbDAO = new TestableDisbursementDAO(jdbcTemplate);

        WorkflowValidator wfVal = new WorkflowValidator();
        BusinessClock clock = new BusinessClock("2026-11-24");
        DisbursementService targetDisbService = new DisbursementService(appDAO, customerDAO, disbDAO, historyDAO, wfVal, clock);

        ProxyFactory pf = new ProxyFactory(targetDisbService);
        pf.addAdvice(new TransactionInterceptor(txManager, new AnnotationTransactionAttributeSource()));
        DisbursementService disbService = (DisbursementService) pf.getProxy();

        // 1. Insert customer
        Customer cust = new Customer();
        cust.setCustomerCode("CUST-001");
        cust.setFullName("Test Customer");
        cust.setMobileNumber("9999999999");
        cust.setDateOfBirth(LocalDate.of(1990, 1, 1));
        cust.setAddressLine("Test Line");
        cust.setCity("Noida");
        cust.setState("UP");
        cust.setPostalCode("201301");
        cust.setKycType("PAN");
        cust.setKycReference("ABCDE1234F");
        cust = customerDAO.save(cust);

        // 2. Insert application in CREDIT_APPROVAL
        LoanApplication app = new LoanApplication();
        app.setApplicationNumber("APP-TEST-001");
        app.setCustomerId(cust.getCustomerId());
        app.setProductType(ProductType.PERSONAL);
        app.setBranchCode("BR001");
        app.setBranchName("Main Branch");
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
        app = appDAO.save(app);

        System.out.println("INITIAL DB STATE:");
        System.out.println("  Disbursements: " + jdbcTemplate.queryForObject("SELECT COUNT(*) FROM disbursement", Integer.class));
        System.out.println("  Status: " + jdbcTemplate.queryForObject("SELECT status FROM loan_application WHERE application_id=" + app.getApplicationId(), String.class));
        System.out.println("  History: " + jdbcTemplate.queryForObject("SELECT COUNT(*) FROM application_history WHERE application_id=" + app.getApplicationId(), Integer.class));

        // Attempt failing disbursement
        historyDAO.fail = true;
        try {
            disbService.disburseLoan(app.getApplicationId(), "TEST_OFFICER");
            System.err.println("FAIL: Expected exception!");
        } catch (Exception e) {
            System.out.println("
EXPECTED FAILURE TRIGGERED: " + e.getMessage());
        }

        System.out.println("
DB STATE AFTER INTENTIONAL FAILURE (ROLLBACK VERIFICATION):");
        System.out.println("  Disbursements: " + jdbcTemplate.queryForObject("SELECT COUNT(*) FROM disbursement", Integer.class));
        System.out.println("  Status: " + jdbcTemplate.queryForObject("SELECT status FROM loan_application WHERE application_id=" + app.getApplicationId(), String.class));
        System.out.println("  History: " + jdbcTemplate.queryForObject("SELECT COUNT(*) FROM application_history WHERE application_id=" + app.getApplicationId(), Integer.class));

        // Attempt successful disbursement
        historyDAO.fail = false;
        disbService.disburseLoan(app.getApplicationId(), "TEST_OFFICER");

        System.out.println("
DB STATE AFTER SUCCESSFUL EXECUTION (COMMIT VERIFICATION):");
        System.out.println("  Disbursements: " + jdbcTemplate.queryForObject("SELECT COUNT(*) FROM disbursement", Integer.class));
        System.out.println("  Status: " + jdbcTemplate.queryForObject("SELECT status FROM loan_application WHERE application_id=" + app.getApplicationId(), String.class));
        System.out.println("  History: " + jdbcTemplate.queryForObject("SELECT COUNT(*) FROM application_history WHERE application_id=" + app.getApplicationId(), Integer.class));
    }
}
