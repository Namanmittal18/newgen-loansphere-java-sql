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
1. Navigate to **Credit Approval** (`/credit`).
2. View approved applications.
3. Click **Download Application Form PDF** to download the official OpenPDF sanction document.
4. Click **Disburse** to move to simulated disbursement.

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
