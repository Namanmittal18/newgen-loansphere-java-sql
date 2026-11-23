# Lending Business Rules & Mathematical Specifications
## Newgen Mini Loan Origination System

---

### 1. Loan Products Configuration

All loan limits, tenures, income criteria, and interest rates are centralized in `LoanProductConfig` and loaded dynamically via `application.properties`:

| Parameter | PERSONAL Loan | EDUCATION Loan | Evaluation / Enforcement |
| :--- | :--- | :--- | :--- |
| **Principal Amount** | ₹50,000 – ₹5,00,000 | ₹1,00,000 – ₹10,00,000 | Inclusive range checked at drafting & submission |
| **Tenure** | 12 – 60 Months | 12 – 84 Months | Whole number integer months within bounds |
| **Min. Net Income** | ₹20,000.00 / month | ₹15,000.00 / month | Strictly positive, verified at submission |
| **Interest Rate** | 12.00% per annum | 10.00% per annum | Configurable parameter |
| **Co-Borrower Rule** | Allowed | Strictly Not Allowed | Assignment policy constraint |

---

### 2. Applicant Age & Validation Rules
- **Completed Age on Submission**:
  $$\text{Age} = \text{Period.between}(\text{dateOfBirth}, \text{businessDate}).\text{getYears}()$$
- **Permissible Range**: $21 \le \text{Age} \le 60$ completed years.
- **Future Dates**: Strictly rejected ($\text{dateOfBirth} > \text{businessDate}$).
- **Mobile Number**: Strictly 10 digits, numeric only, unique across all customers.

---

### 3. Active Application Duplication Rule
- An application is considered **ACTIVE** if its status is:
  $$\text{status} \in \{\text{'DRAFT'}, \text{'UNDER\_REVIEW'}, \text{'CREDIT\_APPROVAL'}\}$$
- **Constraint**: Only **one** active application is allowed per customer per product type.
- Once an application reaches a terminal stage (`REJECTED` or `EXIT`) or is `DISBURSED`, the customer is immediately eligible to apply for another loan of that product.

---

### 4. Mathematical Engine: EMI & FOIR Derivations

#### A. Equated Monthly Installment (EMI) Formula
$$\text{EMI} = P \times r \times \frac{(1+r)^n}{(1+r)^n - 1}$$
Where:
- $P$ = Principal amount (`BigDecimal`).
- $r = \frac{\text{Annual Interest Rate}}{12 \times 100}$ (Monthly fractional rate, calculated with 16-digit `MathContext`).
- $n$ = Tenure in months (`int`).

**Zero Interest Fallback**: If $r = 0$, $\text{EMI} = \frac{P}{n}$.  
**Rounding Policy**: `RoundingMode.HALF_UP` to exactly 2 decimal places.

#### B. Fixed Obligation to Income Ratio (FOIR)
$$\text{FOIR} = \frac{\text{Existing Monthly Obligations} + \text{Rounded EMI}}{\text{Monthly Net Income}} \times 100$$
**Policy Threshold**:
$$\text{FOIR} \le 50.00\% \implies \textbf{Eligible}$$
$$\text{FOIR} > 50.00\% \implies \textbf{Ineligible (Automatic Rejection)}$$

#### C. Worked Verification Example (PERSONAL Loan)
- **Inputs**: $P = \text{₹}1,00,000.00$, $n = 12$, Rate $= 12.00\%$ ($r = 0.01$).
- **Calculated EMI**:
  $$(1.01)^{12} \approx 1.12682503013$$
  $$\text{EMI} = 100000 \times 0.01 \times \frac{1.12682503013}{0.12682503013} \approx \mathbf{8,884.88}$$
- **Case 1 (Eligible)**:
  - Income $= \text{₹}30,000.00$, Obligations $= \text{₹}5,000.00$.
  - Total Monthly Obligation $= 5,000.00 + 8,884.88 = 13,884.88$.
  - $\text{FOIR} = \frac{13884.88}{30000.00} \times 100 = \mathbf{46.28\%} \le 50.00\% \implies \textbf{PASS}$.
- **Case 2 (Ineligible)**:
  - Income $= \text{₹}30,000.00$, Obligations $= \text{₹}7,000.00$.
  - Total Monthly Obligation $= 7,000.00 + 8,884.88 = 15,884.88$.
  - $\text{FOIR} = \frac{15884.88}{30000.00} \times 100 = \mathbf{52.95\%} > 50.00\% \implies \textbf{FAIL}$.

---

### 5. Review Ageing & Overdue Identification
- An application in `UNDER_REVIEW` is monitored relative to the configured business date:
  $$\Delta t = \text{ChronoUnit.DAYS.between}(\text{submittedDate}, \text{businessDate})$$
- **Boundary**:
  - $\Delta t \le 2$ calendar days: **NOT Overdue** (no alert created).
  - $\Delta t > 2$ calendar days (3 days or more): **OVERDUE** (alert `REVIEW_OVERDUE` recorded in `SYSTEM_ALERTS`).
