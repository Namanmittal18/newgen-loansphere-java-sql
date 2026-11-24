# LoanSphere | Mini Loan Origination System
### Newgen – Mini Loan Origination Application | Java & SQL Campus Pre-Onboarding Assignment

---

## 1. Project Overview
**LoanSphere** is an enterprise-grade educational Loan Origination System (LOS) built strictly to fulfill the requirements of the **Newgen Java & SQL Campus Pre-Onboarding Assignment**.

The application orchestrates the entire lending journey:
$$\text{Customer Registration} \longrightarrow \text{Loan Application} \longrightarrow \text{Eligibility Assessment} \longrightarrow \text{Reviewer Decision} \longrightarrow \text{Credit Approval} \longrightarrow \text{Application Form Generation} \longrightarrow \text{Simulated Disbursement} \longrightarrow \text{Core Banking Push} \longrightarrow \text{Exit}$$

---

## 2. Technology Stack
- **Language**: Java 17 or above (Temurin / OpenJDK).
- **Architecture**: Layered MVC (Model, DAO, Service, Controller, Console, Scheduler, Utility).
- **Data Access**: **Pure JDBC** with `PreparedStatement` and Spring `JdbcTemplate` in dedicated DAO classes (**No Hibernate / JPA**).
- **Database**: MySQL 8.x (InnoDB engine, UTF8mb4, relational constraints, foreign keys, indexes).
- **Web UI**: Spring Boot 3.3.x, Thymeleaf, Bootstrap 5, Chart.js.
- **Console UI**: Dedicated 13-menu interactive Java CLI.
- **Financial Calculation Engine**: Pure `BigDecimal` arithmetic with `RoundingMode.HALF_UP` and Indian currency formatting (`₹##,##,###.##`).
- **Document Generation**: OpenPDF for vector-rendered Loan Sanction & Application Form PDF generation.
- **Background Schedulers**: `ScheduledExecutorService` / Spring Schedulers for Overdue Review and CoreDataPush daemons.
- **Testing**: JUnit 5, Mockito, AssertJ (44 automated tests executed & passed; 0 failures, 0 errors; includes live MySQL integration tests & multi-table transaction rollback).

---

## 3. Project Directory Structure
```
Java Project/
├── pom.xml                               # Maven project configuration
├── README.md                             # Main setup & execution guide
├── .env.example                          # Environment variable configuration template
├── database/
│   ├── schema.sql                        # Complete DDL, 7 normalized tables & constraints
│   ├── seed.sql                          # Realistic fictional demonstration data
│   └── reports.sql                       # Parameterized queries for the 6 required SQL reports
├── docs/
│   ├── Design_Document.md                # Comprehensive functional & technical specification
│   ├── ER_Diagram.md                     # Mermaid ER diagram & schema dictionary
│   ├── Test_Cases.md                     # Traceability matrix for all 31 mandatory scenarios
│   ├── Architecture.md                   # Multi-layered architecture & transaction model
│   ├── Business_Rules.md                 # Product limits, age bounds & financial formulas
│   ├── SQL_Reports.md                    # In-depth SQL report explanations
│   ├── User_Guide.md                     # Operational walkthrough for Web & Console
│   └── Presentation_Content.md           # 12-slide technical presentation outline
└── src/
    ├── main/
    │   ├── java/com/newgen/loanorigination/
    │   │   ├── LoanOriginationApplication.java # Spring Boot entry point
    │   │   ├── config/                   # BusinessClock, ProductConfig, DatabaseConfig
    │   │   ├── console/                  # LoanOriginationConsoleApp, ConsoleMenuHandler
    │   │   ├── controller/               # Web controllers for Dashboard, Customers, etc.
    │   │   ├── dao/                      # Pure JDBC DAO interfaces & implementations
    │   │   ├── dto/                      # Data Transfer Objects & Report projections
    │   │   ├── exception/                # Domain-specific custom exceptions
    │   │   ├── model/                    # Domain entities & Enums
    │   │   ├── scheduler/                # OverdueReviewScheduler & CoreBankingScheduler
    │   │   ├── service/                  # Financial, Customer, Workflow, Disbursement services
    │   │   ├── utility/                  # CurrencyFormatter, ReferenceGenerator, CsvExportUtility
    │   │   └── validation/               # CustomerValidator, ProductValidator, WorkflowValidator
    │   └── resources/
    │       ├── application.properties    # Central configuration
    │       └── templates/                # Bootstrap 5 + Thymeleaf HTML views
    └── test/
        └── java/com/newgen/loanorigination/ # JUnit 5 test suites (35 automated unit & integration tests)
```

---

## 4. Database Setup (MySQL 8.x)

1. Open your MySQL 8.x terminal or client:
   ```bash
   mysql -u root -p
   ```
2. Execute the schema script to provision the database and all 7 tables:
   ```sql
   source database/schema.sql;
   ```
3. Execute the seed script to populate realistic fictional demonstration records:
   ```sql
   source database/seed.sql;
   ```
4. Verify all 7 tables:
   ```sql
   SHOW TABLES;
   ```
   You should see:
   - `CUSTOMER`
   - `LOAN_APPLICATION`
   - `DISBURSEMENT`
   - `APPLICATION_HISTORY`
   - `SYSTEM_ALERTS`
   - `UTILITY_RUN`
   - `NG_CORE_BANKING`

---

## 5. Configuration & Environment Variables

Copy `.env.example` or export the variables in your shell:
```bash
export DB_URL="jdbc:mysql://localhost:3306/newgen_loan_origination?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Kolkata"
export DB_USERNAME="root"
export DB_PASSWORD="your_password"
export BUSINESS_DATE="2026-11-23"
```

---

## 6. Running the Application

### Option 1: Web Application (LoanSphere Dashboard)
Run the web server:
```bash
mvn spring-boot:run
```
Open your browser and navigate to:
**`http://localhost:8080/dashboard`**

**Key Web Portals Available**:
- Executive Dashboard: `/dashboard`
- Customer Management: `/customers`
- Loan Applications: `/applications`
- Reviewer Underwriting Queue: `/reviewer`
- Credit Approval & Sanctions: `/credit`
- Simulated Disbursement: `/disbursement`
- 6 SQL Reports Viewer: `/reports`
- Background Utilities Control Center: `/utilities`

---

### Option 2: Menu-Driven Console Application
Run the dedicated console entry point:
```bash
mvn clean compile exec:java -Dexec.mainClass="com.newgen.loanorigination.console.LoanOriginationConsoleApp"
```
Or run the Spring Boot CLI runner:
```bash
java -jar target/loan-origination-system-1.0.0.jar --console
```

**Console Operations (13 Options)**:
1. Customer Management (Register / Search / View)
2. Loan Application Creation (Create DRAFT)
3. Application Submission (Automated Eligibility Assessment)
4. View Applications (List All / View Details)
5. Reviewer Decisions (Approve / Reject with Mandatory Reason)
6. Credit Approval (Sanctions & Verification)
7. Simulated Disbursement (Disburse Approved Loans)
8. SQL Reports (View all 6 reports)
9. Run Overdue Utility ("Run Utility Now")
10. Run CoreDataPush ("Run CoreDataPush Now")
11. View System Alerts
12. View Application Audit History
13. Exit

---

## 7. Running Tests & Boundary Verification

Run the JUnit 5 automated test suite:
```bash
mvn test
```

### Automated Verification Status

- **Automated Unit & Integration Tests**: **44 Passed, 0 Failed, 0 Errors, 0 Skipped** across 11 test classes (`BUILD SUCCESS`).
  - Core Domain & Calculation Unit Tests (32 tests): **PASS**.
  - Multi-Table Transaction Rollback (`TransactionRollbackTest` - 3 tests against real relational tables): **PASS**.
  - Dedicated Live MySQL Integration Suite (`MySqlIntegrationTest` - 9 tests executed against live MySQL 8.x/26.x on port 3306): **PASS**.
    - Covers Scenarios A–I: Schema verification, 7 tables & constraints, duplicate rejection, state machine enforcement, idempotency, and execution of all 6 required SQL reports.
- **Subsequent Phase Verifications Pending**:
  - **PDF automated verification** (Phase 4) – pending automated byte/layout assertion test.
  - **Persistence verification across cold restart** (Phase 5) – pending application context restart verification.

### 31 Mandatory Test Scenarios Traceability

- [x] **01.** PERSONAL minimum loan amount (₹50,000 accepted; ₹49,999 rejected) – `ProductValidatorTest` (**PASS**)
- [x] **02.** PERSONAL maximum loan amount (₹5,00,000 accepted; ₹5,00,001 rejected) – `ProductValidatorTest` (**PASS**)
- [x] **03.** EDUCATION minimum loan amount (₹1,00,000 accepted; ₹99,999 rejected) – `ProductValidatorTest` (**PASS**)
- [x] **04.** EDUCATION maximum loan amount (₹10,00,000 accepted; ₹10,00,001 rejected) – `ProductValidatorTest` (**PASS**)
- [x] **05.** Minimum and maximum tenure boundaries (12 to 60 for Personal; 12 to 84 for Education) – `ProductValidatorTest` (**PASS**)
- [x] **06.** Age 20 completed years on submission – rejected – `CustomerValidatorTest` (**PASS**)
- [x] **07.** Age 21 completed years on submission – accepted – `CustomerValidatorTest` (**PASS**)
- [x] **08.** Age 60 completed years on submission – accepted – `CustomerValidatorTest` (**PASS**)
- [x] **09.** Age 61 completed years on submission – rejected – `CustomerValidatorTest` (**PASS**)
- [x] **10.** Future date of birth – rejected – `CustomerValidatorTest` (**PASS**)
- [x] **11.** Duplicate 10-digit mobile number registration – rejected – `CustomerServiceTest` (**PASS**)
- [x] **12.** Duplicate active application for same customer and product – rejected – `LoanApplicationServiceTest` (**PASS**)
- [x] **13.** New application after rejection or exit – allowed – `LoanApplicationServiceTest` (**PASS**)
- [x] **14.** Worked EMI example exact match: **₹1,00,000 @ 12% for 12 mos = ₹8,884.88** – `FinancialCalculationServiceTest` (**PASS**)
- [x] **15.** FOIR exactly 50.00% – eligible – `FinancialCalculationServiceTest` (**PASS**)
- [x] **16.** FOIR above 50.00% (e.g. 52.95%) – ineligible – `FinancialCalculationServiceTest` (**PASS**)
- [x] **17.** Negative monthly obligations – rejected – `ProductValidatorTest` (**PASS**)
- [x] **18.** Zero or negative monthly net income – rejected – `ProductValidatorTest` (**PASS**)
- [x] **19.** Invalid state transitions (e.g. DRAFT $\to$ DISBURSED) – rejected – `WorkflowValidatorTest` (**PASS**)
- [x] **20.** Missing rejection reason – rejected – `WorkflowValidatorTest` (**PASS**)
- [x] **21.** Duplicate disbursement reference – rejected – `DisbursementServiceTest` (**PASS**)
- [x] **22.** Repeated disbursement on already disbursed application – rejected – `DisbursementServiceTest` (**PASS**)
- [x] **23.** Disbursement before approval – rejected – `DisbursementServiceTest` (**PASS**)
- [x] **24.** Transaction failure – full rollback – `TransactionRollbackTest` (**PASS**)
- [x] **25.** Review age exactly 2 calendar days – not overdue – `OverdueReviewSchedulerTest` (**PASS**)
- [x] **26.** Review age 3 calendar days – overdue – `OverdueReviewSchedulerTest` (**PASS**)
- [x] **27.** Repeated overdue utility execution – no duplicate alerts created – `OverdueReviewSchedulerTest` (**PASS**)
- [x] **28.** Failed core banking push – remains retryable in DISBURSED – `CoreBankingSchedulerTest` (**PASS**)
- [x] **29.** Repeated core banking push – no duplicate rows in NG_CORE_BANKING – `CoreBankingSchedulerTest` (**PASS**)
- [x] **30.** Successful core banking push – transitions to EXIT – `CoreBankingSchedulerTest` (**PASS**)
- [ ] **31.** Application restart – records persist in database (*Phase 5 persistence verification pending*)

---

## 8. Evaluator Verification Guide

1. **Verify Pure JDBC Compliance**:
   Inspect package `com.newgen.loanorigination.dao`. All operations execute raw SQL queries using `JdbcTemplate` and `PreparedStatement`. No Hibernate / JPA annotations or configurations exist.
2. **Verify Financial Precision**:
   Inspect `com.newgen.loanorigination.service.FinancialCalculationService`. All formulas use `BigDecimal` with `MathContext` and `RoundingMode.HALF_UP`.
3. **Verify Transactional Integrity**:
   Inspect `LoanApplicationService`, `ReviewerService`, `DisbursementService`, and `CoreBankingScheduler`. Multi-table updates execute within managed transactions with rollback on failure.
4. **Verify Background Daemons**:
   Inspect `com.newgen.loanorigination.scheduler.OverdueReviewScheduler` and `CoreBankingScheduler`. Both daemons support scheduled execution and instant manual triggers ("Run Utility Now").
