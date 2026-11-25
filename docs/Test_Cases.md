# Test Cases & Verification Evidence Document
## 31 Mandatory Test Scenarios | Newgen Evaluation Rubric

---

### Master Traceability & Verification Matrix

| # | Scenario Description | Test Component / Class | Test Input Data | Expected Result | Actual Result | Status |
| :-: | :--- | :--- | :--- | :--- | :--- | :-: |
| **01** | PERSONAL minimum loan amount | `ProductValidatorTest` | Principal = ₹50,000.00; ₹49,999.00 | ₹50k accepted; ₹49,999 rejected | Match | **PASS** |
| **02** | PERSONAL maximum loan amount | `ProductValidatorTest` | Principal = ₹5,00,000.00; ₹5,00,001.00 | ₹5,00k accepted; ₹5,00,001 rejected | Match | **PASS** |
| **03** | EDUCATION minimum loan amount | `ProductValidatorTest` | Principal = ₹1,00,000.00; ₹99,999.00 | ₹1,00k accepted; ₹99,999 rejected | Match | **PASS** |
| **04** | EDUCATION maximum loan amount | `ProductValidatorTest` | Principal = ₹10,00,000.00; ₹10,00,001.00 | ₹10,00k accepted; ₹10,00,001 rejected | Match | **PASS** |
| **05** | Minimum & maximum tenure | `ProductValidatorTest` | PERSONAL: 11, 12, 60, 61 mos<br>EDUCATION: 84, 85 mos | [12, 60] for Personal accepted; [12, 84] for Education accepted; Out-of-bounds rejected | Match | **PASS** |
| **06** | Age 20 years – reject | `CustomerValidatorTest` | Date of Birth = BusinessDate - 20 years | ValidationException: Must be between 21 and 60 completed years | Match | **PASS** |
| **07** | Age 21 years – accept | `CustomerValidatorTest` | Date of Birth = BusinessDate - 21 years | Validation succeeds; completed age = 21 | Match | **PASS** |
| **08** | Age 60 years – accept | `CustomerValidatorTest` | Date of Birth = BusinessDate - 60 years | Validation succeeds; completed age = 60 | Match | **PASS** |
| **09** | Age 61 years – reject | `CustomerValidatorTest` | Date of Birth = BusinessDate - 61 years | ValidationException: Must be between 21 and 60 completed years | Match | **PASS** |
| **10** | Future DOB – reject | `CustomerValidatorTest` | Date of Birth = BusinessDate + 1 day | ValidationException: DOB cannot be in future | Match | **PASS** |
| **11** | Duplicate mobile – reject | `CustomerServiceTest` | Mobile = `9876543210` (Already exists) | ValidationException: Mobile already registered | Match | **PASS** |
| **12** | Duplicate active application – reject | `LoanApplicationServiceTest`| Customer has active `PERSONAL` app | DuplicateApplicationException thrown | Match | **PASS** |
| **13** | New application after rejection – allow | `LoanApplicationServiceTest`| Customer has prior `REJECTED` or `EXIT` app | New application drafted successfully | Match | **PASS** |
| **14** | Worked EMI example exact match | `FinancialCalculationServiceTest` | P = ₹1,00,000; n = 12; Rate = 12.00% | **EMI = ₹8,884.88** exactly | Match (₹8,884.88) | **PASS** |
| **15** | FOIR exactly 50% – eligible | `FinancialCalculationServiceTest` | Income = ₹30k, Total Obligation = ₹15k | FOIR = 50.00%, eligible = true | Match | **PASS** |
| **16** | FOIR above 50% – ineligible | `FinancialCalculationServiceTest` | Income = ₹30k, Total Obligation = ₹15,884.88 | FOIR = 52.95%, eligible = false | Match | **PASS** |
| **17** | Negative obligations – reject | `ProductValidatorTest` | Obligations = -₹500.00 | Validation failure: Obligations cannot be negative | Match | **PASS** |
| **18** | Zero or negative income – reject | `ProductValidatorTest` | Net income = ₹0.00, -₹1,000.00 | Validation failure: Income must be positive | Match | **PASS** |
| **19** | Invalid state transitions – reject | `WorkflowValidatorTest` | DRAFT $\to$ DISBURSED; UNDER_REVIEW $\to$ DISBURSED | InvalidApplicationStateException thrown | Match | **PASS** |
| **20** | Missing rejection reason – reject | `WorkflowValidatorTest` | Action = REJECT, reason = null or `""` | ValidationException: Rejection requires mandatory reason | Match | **PASS** |
| **21** | Duplicate disbursement reference – reject | `DisbursementServiceTest` | Reference exists in DB | DisbursementException: Reference exists | Match | **PASS** |
| **22** | Repeated disbursement – reject | `DisbursementServiceTest` | Loan already has a disbursement record | DisbursementException: Already disbursed | Match | **PASS** |
| **23** | Disbursement before approval – reject | `DisbursementServiceTest` | App status = `UNDER_REVIEW` / `DRAFT` | InvalidApplicationStateException thrown | Match | **PASS** |
| **24** | Transaction failure – full rollback | `TransactionRollbackTest` | Exception thrown during multi-table update | No partial history, disbursement, or status change | Match | **PASS** |
| **25** | Review age exactly 2 days – not overdue | `OverdueReviewSchedulerTest` | Submitted: Nov 24; Checked: Nov 26 | Days waiting = 2; `isOverdue = false`; No alert created | Match | **PASS** |
| **26** | Review age 3 days – overdue | `OverdueReviewSchedulerTest` | Submitted: Nov 23; Checked: Nov 26 | Days waiting = 3; `isOverdue = true`; Alert created | Match | **PASS** |
| **27** | Repeated overdue utility – no duplicates | `OverdueReviewSchedulerTest` | Run utility second time | Unique constraint triggers; 0 new alerts | Match | **PASS** |
| **28** | Failed core banking push – remains retryable | `CoreBankingSchedulerTest` | DB failure during sync | Application remains `DISBURSED`, retryable | Match | **PASS** |
| **29** | Repeated core banking push – no duplicates | `CoreBankingSchedulerTest` | Already pushed record claimed | Unique key prevents insert; marked EXIT | Match | **PASS** |
| **30** | Successful core banking push – EXIT | `CoreBankingSchedulerTest` | Disbursed loan processed | Record in `NG_CORE_BANKING`, status = EXIT | Match | **PASS** |
| **31** | Restart application – records persist | `ColdRestartPersistenceTest` | Restart context & physical MySQL daemon | All applications, customers, ledgers persist across cold restart | Match | **PASS** |

---

### Verification Execution Status (All 31 Scenarios & Phase 5 Cold Restart Passed)
- **Automated Tests Executed**: **55 Passed, 0 Failed, 0 Errors, 0 Skipped** across 13 test suites (`BUILD SUCCESS`).
- **Master Test Coverage Summary**:
  - **All 31 Mandatory Evaluation Scenarios**: **100% PASS**.
  - **Dedicated Live MySQL Integration Suite** (`MySqlIntegrationTest`): 9 tests covering Scenarios A–I and all 6 analytical SQL reports (**PASS**).
  - **Multi-Table Transaction Rollback Suite** (`TransactionRollbackTest`): 3 tests verifying atomic rollback across database tables (**PASS**).
  - **Application Form PDF Verification Suite** (`ApplicationFormPdfTest`): 8 tests verifying OpenPDF layout, fields, and endpoints (**PASS**).
  - **Cold Restart Database Persistence Suite** (`ColdRestartPersistenceTest`): 2 tests verifying full lifecycle persistence across independent connection pools, scheduler idempotency, and physical MySQL service cold restart (**PASS**).
  - **Financial Calculation & Currency Suite** (`FinancialCalculationServiceTest`): 5 tests including Indian currency formatting regression (**PASS**).

---

### PDF Document Automated Test Scenarios Matrix

| Scenario ID | Test Method | Objective & Inputs | Assertions & Extracted Fields Verified | Status |
|---|---|---|---|:---:|
| **PDF-01** | `testValidPersonalLoanPdfContent` | Personal loan (`APP-2026-00101`), ₹2.5L, 36 mos, 12%, FOIR 22.17%, status `CREDIT_APPROVAL` | `%PDF-` header, App No, Status, Branch, Dates, Customer Name, DOB & Age, Mobile, KYC, Address, Currency formatting (INR), EMI, FOIR, Sanction Signatures | **PASS** |
| **PDF-02** | `testValidEducationLoanPdfContent` | Education loan (`APP-2026-00202`), ₹8L, 60 mos, 9.5%, FOIR 31.53%, status `DISBURSED` | `EDUCATION` product tag, ₹8L amount, 60 months tenure, 9.50% p.a. rate, `DISBURSED` state, calculated EMI INR 16,801.32 | **PASS** |
| **PDF-03** | `testLongCustomerNameAndAddress` | Long customer name (63 chars) & lengthy address (160+ chars) | Auto-wrapping in `PdfPCell`, no clipping/truncation, valid page count ($\ge 1$), complete text extracted | **PASS** |
| **PDF-04** | `testDifferentApplicationStatuses` | `DRAFT`, `UNDER_REVIEW`, `REJECTED`, `EXIT` | Sanctioned amount "N/A (Pending Sanction)" for unapproved loans; Rejection reason displayed in sanction remarks for `REJECTED` | **PASS** |
| **PDF-05** | `testNonexistentApplicationId` | Invalid App ID (`999999L`) requested via `/credit/{id}/pdf` | Handled via `ApplicationNotFoundException`; 404 response | **PASS** |
| **PDF-06** | `testIncompleteAndNullDataGracefulHandling` | Null `LoanApplication`, null customer, missing dates | Null application throws `IllegalArgumentException`; null customer defaults gracefully to `"N/A"` placeholders without NPE | **PASS** |
| **PDF-07** | `testPhysicalFileWriteAndReadability` | Write PDF bytes to `@TempDir` file on physical disk | File exists, size > 1 KB, re-opened and parsed by `PdfReader`, valid PDF version (1.4/1.5) | **PASS** |
| **PDF-08** | `testCreditApprovalControllerDownloadEndpoint` | HTTP GET `/credit/{id}/pdf` | HTTP 200 OK, `Content-Type: application/pdf`, `Content-Disposition: attachment; filename="ApplicationForm_APP-...pdf"`, valid PDF body bytes | **PASS** |

---

### Boundary Verification Summary
- **Age Bounds**: Age 20: Rejected | Age 21: Accepted | Age 60: Accepted | Age 61: Rejected.
- **Review Ageing Bounds**: Age 2 calendar days: Not Overdue | Age 3 calendar days: Overdue.
- **Affordability Bounds**: FOIR 50.00%: Accepted | FOIR 50.01%: Rejected.
- **Idempotency**: All background utilities verified crash-safe and duplicate-immune.
