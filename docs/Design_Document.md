# LoanSphere – Mini Loan Origination System
## Comprehensive System Design Document
**Pre-Onboarding Campus Assignment | Newgen Software Technologies Ltd.**

---

### 1. Project Background & Problem Statement
In commercial retail lending, a Loan Origination System (LOS) manages the end-to-end journey of a borrower from customer registration, loan drafting, and automated credit assessment through underwriting review, sanction, simulated disbursement, and downstream core banking archival.

**LoanSphere** is an enterprise-grade, educational Loan Origination System designed to model the canonical lending lifecycle with rigorous database constraints, transactional atomicity, explainable Java 17+ code, and pure JDBC data access.

---

### 2. Functional Scope & Lending Lifecycle
The system enforces the following sequential stages:
$$\text{DRAFT} \longrightarrow \text{UNDER\_REVIEW} \longrightarrow \text{CREDIT\_APPROVAL} \longrightarrow \text{DISBURSED} \longrightarrow \text{CORE\_BANKING\_PUSH} \longrightarrow \text{EXIT}$$

#### Stage Descriptions:
1. **DRAFT**:
   - Customer selection, loan product selection (PERSONAL or EDUCATION), branch details.
   - Financial input capture: requested amount, tenure, net monthly income, existing monthly obligations.
   - Real-time preview of Equated Monthly Installment (EMI) and Fixed Obligation to Income Ratio (FOIR).
   - Only DRAFT applications can be modified or submitted.
2. **UNDER\_REVIEW**:
   - Automated eligibility validation engine executes on submission:
     - Applicant age validation: $[21, 60]$ completed years on submission date.
     - 10-digit unique mobile check.
     - Product boundary limits (amount, tenure, minimum income).
     - Active application duplication check (1 active per customer per product).
     - Exact $\text{FOIR} \le 50.00\%$ affordability policy.
   - If eligible: transitions to `UNDER_REVIEW`.
   - If ineligible: transitions to `REJECTED` with all failed rules persisted.
   - Underwriting review: credit reviewer reviews profile and decides `CREDIT_APPROVAL` or `REJECTED` (with mandatory reason).
3. **CREDIT\_APPROVAL**:
   - Corresponds to the sanctioned stage awaiting credit finalization and disbursement.
   - Official Application Form PDF generated using OpenPDF.
   - Allows rejection by credit authority with mandatory justification.
4. **DISBURSED**:
   - Simulated disbursement executes atomically:
     - Globally unique disbursement reference generated (`DISB-YYYYMMDD-XXXXXX`).
     - Exactly one disbursement permitted per loan application.
     - Ledger record inserted into `DISBURSEMENT` table.
     - Status updated to `DISBURSED` and audit history logged.
5. **CORE\_BANKING\_PUSH**:
   - Automated background utility (`CoreBankingScheduler`) claims disbursed records.
   - Synchronizes customer KYC, address, approved amount, and disbursement details into `NG_CORE_BANKING`.
6. **EXIT**:
   - Terminal stage automatically reached upon verified core banking synchronization.
   - Idempotent and crash-resilient.

---

### 3. Technology Stack & Design Principles
- **Programming Language**: Java 17+ (Temurin / OpenJDK).
- **Persistence Layer**: Pure JDBC using `PreparedStatement` and Spring's `JdbcTemplate`. No ORM (Hibernate/JPA) abstractions.
- **Database**: MySQL 8.x (with support for embedded H2 MySQL mode for isolated zero-config testing).
- **Web Application Layer**: Spring Boot 3.3.x, Thymeleaf, Bootstrap 5, Chart.js.
- **Console Application Layer**: Dedicated menu-driven interactive Java CLI (13 operational menus).
- **Document Generation**: OpenPDF for vector-rendered Loan Sanction & Application Form PDF generation.
- **Schedulers**: `ScheduledExecutorService` / Spring Task Execution for Overdue Review and CoreDataPush daemons.
- **Testing**: JUnit 5, Mockito, AssertJ covering all 31 mandatory assignment scenarios.

---

### 4. Non-Functional & Architecture Requirements
- **Data Integrity**: Enforced using relational database foreign keys, unique constraints (`application_id` on disbursement and core banking; `application_id, alert_type` on alerts), and check constraints.
- **Financial Precision**: All monetary values computed using `BigDecimal` with 16-digit mathematical context and scaled to 2 decimal places with `RoundingMode.HALF_UP`.
- **Currency Representation**: Indian numbering convention formatted as `₹##,##,###.##`.
- **Auditability**: Every stage change and administrative intervention is recorded in `APPLICATION_HISTORY` with user code, timestamps, old status, new status, and remarks inside the same transaction.
- **Idempotency**: Schedulers and background workers can be executed repeatedly without duplicate alerts or redundant database entries.
