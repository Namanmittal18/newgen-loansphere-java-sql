# Technical Presentation Content (12-Slide Outline)
## Evaluation Deck: Mini Loan Origination Application ("LoanSphere")
**Campus Pre-Onboarding Assignment | Newgen Software Technologies Ltd.**

---

### Slide 1: Title Slide
- **Title**: LoanSphere – Mini Loan Origination System
- **Subtitle**: End-to-End Lending Lifecycle, Pure JDBC Architecture & Database Integrity
- **Author**: Campus Pre-Onboarding Hire
- **Organization**: Newgen Software Technologies Ltd.
- **Key Highlights**: Java 17+, MySQL 8.x, Pure JDBC (No Hibernate/JPA), BigDecimal Financial Precision, Dual Console & Web UI, Automated Daemons.

---

### Slide 2: Problem Statement
- In commercial banking and microfinance, loan origination requires:
  - Strict compliance with applicant age and KYC documentation.
  - Accurate reducing-balance EMI and FOIR affordability calculations.
  - Multi-stage underwriting governance with immutable audit logging.
  - Real-time fraud/duplication prevention across active loan pipelines.
  - Reliable reconciliation with downstream Core Banking systems (`NG_CORE_BANKING`).
- Goal: Engineer a robust, educational, production-grade lending application demonstrating software engineering discipline and relational database integrity.

---

### Slide 3: Project Objectives
- Implement the complete lending journey:
  $$\text{Registration} \to \text{Application} \to \text{Eligibility} \to \text{Underwriting} \to \text{Sanction} \to \text{Disbursement} \to \text{Core Push} \to \text{Exit}$$
- Adhere strictly to the Newgen Pre-Onboarding specification:
  - Pure JDBC with PreparedStatements in dedicated DAO repositories.
  - Zero Hibernate/JPA abstractions.
  - Strict database transaction management with rollback.
  - Scheduled background daemons using `ScheduledExecutorService`.
  - Comprehensive unit and integration test coverage across all 31 boundary scenarios.

---

### Slide 4: Technology Stack
- **Core Platform**: Java 17+ (Temurin / OpenJDK LTS).
- **Persistence**: Pure JDBC (`PreparedStatement`, `JdbcTemplate`, `DataSourceTransactionManager`).
- **Database**: MySQL 8.x (InnoDB, UTF8mb4, relational constraints, foreign keys, indexes).
- **Web Layer**: Spring Boot 3.3.x, Thymeleaf, Bootstrap 5, Chart.js.
- **Console Interface**: Interactive 13-menu Java CLI using shared service layer.
- **Document Generation**: OpenPDF for vector-rendered Loan Sanction PDFs.
- **Testing Framework**: JUnit 5, Mockito, AssertJ.

---

### Slide 5: Layered System Architecture
- **Layer Separation**:
  - Presentation Layer: Responsive Web Dashboard + Interactive Console.
  - Controller Layer: REST/MVC endpoints delegating cleanly to services.
  - Service Layer: Pure business logic (Loan workflow, Customer validation, EMI/FOIR engine).
  - Validation Layer: Age $[21, 60]$, 10-digit mobile, product boundaries, state transitions.
  - Scheduler Layer: Daily Overdue Reviewer daemon + Continuous CoreDataPush daemon.
  - Data Access Layer (DAO): Explicit SQL queries in dedicated repository classes.
  - Database Layer: 7 normalized MySQL tables with foreign keys and check constraints.

---

### Slide 6: Loan Origination Lifecycle & State Machine
- **Canonical 7 Stages**:
  - `DRAFT` $\to$ Initial capture of financial details & live preview.
  - `UNDER_REVIEW` $\to$ Automatic eligibility verified; awaiting reviewer decision.
  - `CREDIT_APPROVAL` $\to$ Reviewer approved; official Application Form PDF generated.
  - `DISBURSED` $\to$ Unique disbursement reference generated; full loan disbursed.
  - `CORE_BANKING_PUSH` $\to$ System stage; daemon claims disbursed record.
  - `EXIT` $\to$ Successfully inserted into `NG_CORE_BANKING`; terminal state.
  - `REJECTED` $\to$ Eligibility failure or reviewer rejection with mandatory reason; terminal state.
- **Strict Transition Enforcement**: Invalid transitions are unconditionally rejected.

---

### Slide 7: Database Design & ER Diagram
- Normalized 7-table MySQL schema:
  - `CUSTOMER` (Master profile, unique mobile, DOB, KYC, address).
  - `LOAN_APPLICATION` (Product, financial inputs, calculated EMI/FOIR, reviewer codes).
  - `DISBURSEMENT` (`UNIQUE(application_id)` enforces 1 disbursement per loan; unique reference).
  - `APPLICATION_HISTORY` (Immutable audit trail with timestamps and remarks).
  - `SYSTEM_ALERTS` (`UNIQUE(application_id, alert_type)` prevents duplicate overdue alerts).
  - `UTILITY_RUN` (Daemon execution ledger).
  - `NG_CORE_BANKING` (`UNIQUE(application_id)` guarantees idempotent core banking sync).

---

### Slide 8: Financial Calculations: EMI & FOIR
- **EMI Reducing Balance Formula**:
  $$\text{EMI} = P \times r \times \frac{(1+r)^n}{(1+r)^n - 1}$$
  - $P$ = Principal, $r$ = monthly rate, $n$ = months.
  - Calculated with 16-digit `MathContext`, rounded to 2 decimals using `HALF_UP`.
- **Fixed Obligation to Income Ratio (FOIR)**:
  $$\text{FOIR} = \frac{\text{Existing Monthly Obligations} + \text{Rounded EMI}}{\text{Monthly Net Income}} \times 100$$
  - Threshold: $\text{FOIR} \le 50.00\%$ is Eligible; $> 50.00\%$ is Ineligible.
- **Assignment Verification Match**:
  - Personal: ₹1,00,000 / 12 mo @ 12% = **₹8,884.88 EMI**.
  - Income ₹30,000, Obligations ₹5,000 = **46.28% FOIR (Eligible)**.
  - Income ₹30,000, Obligations ₹7,000 = **52.95% FOIR (Ineligible)**.

---

### Slide 9: Core Application Functionality & Demonstration
- **Dual User Interfaces**:
  - Web Application: Polished Navy/Slate SaaS UI with live calculation preview, status charts, and modals.
  - Console Application: Comprehensive 13-option CLI with input validation.
- **Sanction PDF Generation**:
  - Real-time OpenPDF document rendering official sanction letter and signature blocks.
- **Simulated Disbursement**:
  - Atomic ledger generation with unique reference `DISB-YYYYMMDD-XXXXXX`.

---

### Slide 10: Six Required SQL Reports
1. **Application Register**: Filtered by product and status.
2. **Product Pipeline**: Multi-dimensional aggregation grouped by Product/Status and Branch/Product.
3. **Disbursement Summary**: Date-range filtered daily, monthly, and quarterly aggregations with branch breakdown.
4. **Overdue Reviews**: Current `UNDER_REVIEW` applications older than 2 calendar days.
5. **Zero Disbursement Customers**: Anti-join returning customers who have applied but never disbursed.
6. **Repeat Applicants**: Computed using `GROUP BY customer_id HAVING COUNT(application_id) > 1`.

---

### Slide 11: Background Utilities & Transaction Integrity
- **Overdue Review Utility**:
  - Runs daily via `ScheduledExecutorService` (and on-demand).
  - Ageing rule: $> 2$ calendar days = overdue (Day 2 = Not overdue; Day 3 = Overdue).
  - Generates date-stamped CSV extract: `pending_reviews_YYYYMMDD.csv`.
  - Idempotent: `UNIQUE(application_id, alert_type)` prevents duplicate alerts.
- **CoreDataPush Utility**:
  - Continuous daemon claiming `DISBURSED` loans.
  - Pushes into `NG_CORE_BANKING` and advances to `EXIT` atomically.
  - Crash-safe: Failures leave loans in `DISBURSED` for safe retry without data loss.

---

### Slide 12: Testing, Verification & Conclusion
- **Rigorous Verification**:
  - 31 mandatory assignment scenarios tested and verified.
  - 100% boundary compliance (Ages 20, 21, 60, 61; FOIR 50% vs 50.01%; Review ages 2 vs 3 days).
  - Transaction failure rollback verified.
- **Conclusion**:
  - LoanSphere fulfills every requirement of the Newgen Campus Pre-Onboarding Assignment.
  - Clean, explainable, production-style Java and SQL codebase ready for evaluator demonstration.
