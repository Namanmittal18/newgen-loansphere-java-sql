# System Architecture & Technical Specifications
## Layered Architecture of LoanSphere

---

### 1. Architectural Layers & Separation of Concerns

LoanSphere follows a strict multi-tiered layered architecture ensuring clean separation of concerns, testability, and enterprise maintainability:

```
+-------------------------------------------------------------------------------+
|                             PRESENTATION LAYER                                |
|   [Thymeleaf + Bootstrap 5 Web UI]     |     [Menu-Driven Console Application]|
+-------------------------------------------------------------------------------+
                                      |
                                      v
+-------------------------------------------------------------------------------+
|                              CONTROLLER LAYER                                 |
|   DashboardController, CustomerController, LoanApplicationController,          |
|   ReviewerController, CreditApprovalController, DisbursementController,       |
|   ReportController, UtilityController                                         |
+-------------------------------------------------------------------------------+
                                      |
                                      v
+-------------------------------------------------------------------------------+
|                               SERVICE LAYER                                   |
|   FinancialCalculationService (Exact BigDecimal EMI/FOIR Formulas)             |
|   CustomerService (Age/Mobile Validations & Registration)                      |
|   LoanApplicationService (Workflow, Eligibility, Active App Constraints)       |
|   ReviewerService (Underwriting, Approve/Reject Decisions)                     |
|   DisbursementService (Atomic Disbursement & Unique Reference Generation)      |
|   PdfGenerationService (OpenPDF Sanction Letter Generation)                   |
|   ReportService (Exposing the 6 Parameterized Reports)                         |
+-------------------------------------------------------------------------------+
                    |                                           |
                    v                                           v
+---------------------------------------+   +-----------------------------------+
|            SCHEDULER LAYER            |   |         VALIDATION LAYER          |
|   OverdueReviewScheduler (Daily + CSV)|   |   CustomerValidator (Age 21-60)   |
|   CoreBankingScheduler (Continuous)   |   |   ProductValidator (Limits)       |
+---------------------------------------+   |   WorkflowValidator (Transitions) |
                    |                       +-----------------------------------+
                    v                                           |
+---------------------------------------------------------------+
|                       DATA ACCESS LAYER (DAO)                         |
|   CustomerDAO, LoanApplicationDAO, DisbursementDAO,                   |
|   ApplicationHistoryDAO, SystemAlertDAO, UtilityRunDAO,               |
|   CoreBankingDAO, ReportDAO                                           |
|   (Pure JDBC with PreparedStatement & JdbcTemplate; No JPA/Hibernate) |
+-----------------------------------------------------------------------+
                                      |
                                      v
+-------------------------------------------------------------------------------+
|                         DATABASE LAYER (MySQL 8.x)                            |
|   CUSTOMER, LOAN_APPLICATION, DISBURSEMENT, APPLICATION_HISTORY,              |
|   SYSTEM_ALERTS, UTILITY_RUN, NG_CORE_BANKING                                 |
+-------------------------------------------------------------------------------+
```

---

### 2. Transaction Management & Rollback Strategy

1. **Transactional Boundaries**:
   - `LoanApplicationService.submitApplication()`: Updates application status, sets submission timestamp, inserts eligibility reasons, and inserts `APPLICATION_HISTORY` inside a single atomic database transaction.
   - `ReviewerService.processReviewDecision()`: Updates application status, records reviewer code, decision reason, approved amount, approved timestamp, and inserts `APPLICATION_HISTORY` atomically.
   - `DisbursementService.disburseLoan()`: Inserts `DISBURSEMENT` record, updates application status to `DISBURSED`, and logs `APPLICATION_HISTORY` within a single commit/rollback block.
   - `CoreBankingScheduler.processSingleCoreBankingPush()`: Inserts record into `NG_CORE_BANKING`, transitions application status to `EXIT`, and logs history within a dedicated atomic transaction.
2. **Rollback Behavior**:
   - Uses `PlatformTransactionManager` and Spring's `TransactionTemplate` / `@Transactional`.
   - Any runtime failure or database constraint violation triggers a full rollback (`ROLLBACK WORK`).
   - Partial history records or orphaned disbursements are completely prevented.

---

### 3. Design Patterns Employed
1. **Data Access Object (DAO) Pattern**: Decouples business logic from SQL dialect and prepared statements.
2. **Strategy / Provider Pattern**: `BusinessClock` provides configurable date/time abstraction for time-travel and boundary testing.
3. **Template Method Pattern**: `TransactionTemplate` encapsulates transaction lifecycle (begin, commit, rollback on exception).
4. **Data Transfer Object (DTO) Pattern**: Decouples API/presentation models from underlying relational schema.
