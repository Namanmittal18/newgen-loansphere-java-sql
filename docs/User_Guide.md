# User Guide & Operational Manual
## Operating LoanSphere (Web & Console)

---

### 1. Launching the Application

#### Option A: Interactive Menu-Driven Console Application
Run the dedicated console application:
```bash
# Using Maven:
mvn clean compile exec:java -Dexec.mainClass="com.newgen.loanorigination.console.LoanOriginationConsoleApp"

# Or via the packaged JAR:
java -cp target/loan-origination-system-1.0.0.jar com.newgen.loanorigination.console.LoanOriginationConsoleApp
```
**Console Menu Structure**:
```
 1. Customer Management (Register / Search / View)
 2. Loan Application Creation (Create DRAFT)
 3. Application Submission (Submit for Automated Eligibility)
 4. View Applications (List All / View Details)
 5. Reviewer Decisions (Approve / Reject Under Review)
 6. Credit Approval (Sanction Management)
 7. Simulated Disbursement (Disburse Approved Loans)
 8. SQL Reports (All 6 Parameterized Reports)
 9. Run Overdue Utility ('Run Utility Now')
10. Run CoreDataPush ('Run CoreDataPush Now')
11. View System Alerts
12. View Application Audit History
13. Exit
```

#### Option B: Enterprise Web Dashboard
Run the Spring Boot web application:
```bash
mvn spring-boot:run
```
Access the dashboard in your web browser:
`http://localhost:8080/dashboard`

---

### 2. Step-by-Step Lending Workflow Guide

#### Step 1: Customer Registration
1. Navigate to **Customers** $\to$ **Register New Customer** (`/customers/new`).
2. Input legal name, date of birth (applicant must be 21–60 completed years old), 10-digit unique mobile, residential address, and KYC details.
3. Click **Register Customer**. Customer code `CUST-YYYYMMDD-XXXX` is generated.

#### Step 2: Create Loan Application (DRAFT)
1. Navigate to **Applications** $\to$ **New Application** (`/applications/new`).
2. Select Customer, Product (`PERSONAL` or `EDUCATION`), and Branch.
3. Enter requested amount, tenure in months, net monthly income, and existing obligations.
4. Observe the **Live Calculation Preview**:
   - Proposed Monthly EMI (reducing balance formula).
   - Fixed Obligation to Income Ratio (FOIR).
   - Real-time eligibility badge ($\text{FOIR} \le 50.00\%$).
5. Click **Create DRAFT Application**.

#### Step 3: Application Submission & Eligibility Assessment
1. Open the application details page.
2. Click **Submit for Eligibility Assessment**.
3. The automated eligibility engine evaluates:
   - Applicant age on submission date.
   - Product amount and tenure limits.
   - Monthly income threshold.
   - Active loan concurrency constraint.
   - $\text{FOIR} \le 50.00\%$.
4. **Outcomes**:
   - If eligible: transitions to `UNDER_REVIEW`.
   - If ineligible: transitions to `REJECTED` and persists all specific failure reasons.

#### Step 4: Underwriting & Reviewer Decision
1. Navigate to **Reviewer Workspace** (`/reviewer`).
2. Select an application in `UNDER_REVIEW` to evaluate.
3. Review applicant credit profile and financial ratios.
4. Input Reviewer Staff Code (e.g. `REV-101`).
5. Choose decision:
   - **APPROVE**: Advances status to `CREDIT_APPROVAL`.
   - **REJECT**: Mandatory rejection reason required; transitions to terminal `REJECTED`.

#### Step 5: Credit Approval & PDF Application Form Generation
1. Navigate to **Credit Approval** (`/credit`) in the Web UI, or select Option `6. Credit Approval (Sanctions & Verification)` in the interactive Console.
2. View approved applications pending sanction.
3. **Web Download**: Click **Download Application Form PDF** (or the red PDF icon) on any application in the table or view page to trigger `GET /credit/{id}/pdf`. The browser downloads an official vector-rendered OpenPDF document named `ApplicationForm_{appNo}.pdf`.
4. **Console Export**: In Console Menu Option 6, after viewing or reviewing an application, answer `Y` to the prompt:
   ```text
   Do you want to export official Loan Application Form PDF? (Y/N): Y
   [✓] PDF Application Form exported successfully to: ApplicationForm_APP-202611-1.pdf
   ```
5. Click **Disburse** (or proceed in Console Option 7) to move to simulated disbursement.

#### Step 6: Simulated Loan Disbursement
1. Navigate to **Disbursement** (`/disbursement`).
2. Click **Execute Disbursement**.
3. System verifies `CREDIT_APPROVAL` status, generates globally unique reference `DISB-YYYYMMDD-XXXXXX`, inserts ledger record, moves application to `DISBURSED`, and logs audit trail.
4. Instant confirmation banner displays:
   - Application Number
   - Customer Full Name
   - Disbursed Amount
   - Disbursement Reference ID
   - Timestamp

#### Step 7: Core Banking Integration (CoreDataPush)
1. The background worker continuously claims `DISBURSED` cases.
2. Or navigate to **Utilities** (`/utilities`) and click **Run CoreDataPush Now**.
3. Disbursement and KYC records are inserted into `NG_CORE_BANKING`.
4. Application automatically advances to terminal state `EXIT`.

#### Step 8: Overdue Review Monitoring
1. Navigate to **Utilities** (`/utilities`).
2. Click **Run Utility Now** (optionally input a simulated business date for time travel testing).
3. Applications in `UNDER_REVIEW` older than 2 calendar days are flagged with `REVIEW_OVERDUE` in `SYSTEM_ALERTS` without changing loan status.
4. CSV extract `pending_reviews_YYYYMMDD.csv` is generated in `./exports`.

---

### 3. MySQL Database Setup & Live Integration Guide

#### 3.1 Prerequisites for MySQL Execution
1. Install MySQL 8.x (e.g. via Homebrew on macOS):
   ```bash
   brew install mysql
   brew services start mysql
   ```
2. Create and provision the database:
   ```bash
   mysql -u root -p
   ```
   ```sql
   source database/schema.sql;
   source database/seed.sql;
   ```

#### 3.2 Running Live MySQL Integration Tests
Once the MySQL server daemon is active on port 3306:
```bash
# Override database credentials if needed:
export DB_URL="jdbc:mysql://localhost:3306/newgen_loan_origination?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Kolkata"
export DB_USERNAME="root"
export DB_PASSWORD="your_password"

# Execute test suite:
mvn test -Dtest=MySqlIntegrationTest
```
*Note: When MySQL is offline, live MySQL tests are safely skipped via JUnit 5 dynamic assumptions without blocking unit test builds.*

---

### 4. Loan Application Form PDF Specification & Verification

#### 4.1 Document Content Layout
The generated PDF strictly conforms to standard enterprise banking sanction letters:
- **Header**: Application Title & Institutional Sub-banner.
- **Section 1: Application Summary**: Application Number, Current Workflow Status, Product Type (`PERSONAL` / `EDUCATION`), Branch Code & Name, Submission Date, Approval Date.
- **Section 2: Applicant Information**: Customer Full Name, Customer Unique Code, Date of Birth & Completed Age in Years (e.g. `15-Jun-1995 (Age: 31 years)`), 10-digit Mobile Number, KYC Type & Reference, KYC Verification Status, Multi-line Residential Address (auto-wrapped without clipping).
- **Section 3: Financial Terms & Affordability**: Requested Loan Amount (Formatted in Indian comma format `INR ##,##,###.##`), Sanctioned Loan Amount, Tenure in Months, Annual Interest Rate (`% p.a.`), Monthly Net Income, Existing Obligations, Calculated Equated Monthly Installment (EMI), FOIR Percentage with benchmark comparison (`FOIR% (Limit: 50.00%)`).
- **Section 4: Reviewer Approval & Sanctions**: Reviewing Officer Code, Sanction Remarks or Underwriting Reason.
- **Signatures Block**: Pre-aligned signature areas for Applicant and Sanction Authority.

#### 4.2 Automated Testing
Run the dedicated automated PDF test suite:
```bash
mvn test -Dtest=ApplicationFormPdfTest
```
This executes 8 end-to-end programmatic verification tests using OpenPDF `PdfReader` and `PdfTextExtractor`, inspecting PDF headers (`%PDF-1.5`), byte structure, data non-truncation, multi-status rendering, null safety, temp disk writes, and REST endpoints.

---

### 5. Cold Restart Database Persistence Verification

#### 5.1 Verification Principle
LoanSphere is designed for multi-tier enterprise persistence. All state transitions, customer registrations, sanction records, disbursement ledgers, core banking payloads, and audit trails persist permanently across:
1. Application server termination / context unloads.
2. MySQL daemon cold restarts (`brew services stop mysql` followed by `brew services start mysql`).
3. Connection pool teardowns and reconnections.

#### 5.2 Automated Persistence Test Execution
Run the automated cold restart persistence suite:
```bash
mvn test -Dtest=ColdRestartPersistenceTest
```
This test suite:
- Bootstraps Application Context A against live MySQL `newgen_loan_origination`.
- Executes the full lifecycle: Customer registration $\to$ Application submission $\to$ Underwriting approval $\to$ Disbursement ledger insertion $\to$ CoreDataPush integration $\to$ `EXIT` terminal state.
- Completely tears down Context A and all connection pool handles.
- Re-initializes Application Context B from scratch.
- Verifies that all 7 tables maintain 100% data fidelity, primary/foreign key integrity, and timestamps.
- Verifies that scheduler executions on restarted applications remain strictly idempotent with zero duplicate entries.
