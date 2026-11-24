# Six Standard SQL Reports Documentation
## File: `database/reports.sql` | Newgen Loan Origination Assignment

---

### Report 1: Application Register
- **Business Purpose**: Comprehensive register of all loan originations across products and stages.
- **Parameters**: `?` (Product filter or empty for all), `?` (Status filter or empty for all).
- **SQL Implementation**:
```sql
SELECT 
    la.application_number,
    c.full_name AS customer_name,
    la.product_type,
    la.requested_amount,
    la.proposed_emi,
    la.status,
    la.submitted_at
FROM LOAN_APPLICATION la
JOIN CUSTOMER c ON la.customer_id = c.customer_id
WHERE (? IS NULL OR ? = '' OR la.product_type = ?)
  AND (? IS NULL OR ? = '' OR la.status = ?)
ORDER BY la.created_at DESC;
```

---

### Report 2: Product Pipeline
- **Business Purpose**: Portfolio exposure and volume breakdown grouped by product, status, and branch.
- **SQL Implementation 2A (Product & Status)**:
```sql
SELECT 
    la.product_type,
    la.status,
    COUNT(la.application_id) AS application_count,
    COALESCE(SUM(la.requested_amount), 0.00) AS total_requested_amount
FROM LOAN_APPLICATION la
GROUP BY la.product_type, la.status
ORDER BY la.product_type, la.status;
```
- **SQL Implementation 2B (Branch & Product)**:
```sql
SELECT 
    la.branch_code,
    la.branch_name,
    la.product_type,
    COUNT(la.application_id) AS application_count,
    COALESCE(SUM(la.requested_amount), 0.00) AS total_requested_amount
FROM LOAN_APPLICATION la
GROUP BY la.branch_code, la.branch_name, la.product_type
ORDER BY la.branch_code, la.product_type;
```

---

### Report 3: Daily / Monthly / Quarterly Disbursement Summary
- **Business Purpose**: Financial reconciliation of disbursed funds across daily, monthly, or quarterly periods with branch-level attribution.
- **Parameters**: `?` (Start Timestamp), `?` (End Timestamp).
- **SQL Implementation (Monthly Example)**:
```sql
SELECT 
    DATE_FORMAT(d.disbursed_at, '%Y-%m') AS disbursement_period,
    la.branch_code,
    la.branch_name,
    COUNT(d.disbursement_id) AS disbursement_count,
    COALESCE(SUM(d.amount), 0.00) AS total_disbursed_amount
FROM DISBURSEMENT d
JOIN LOAN_APPLICATION la ON d.application_id = la.application_id
WHERE d.disbursed_at >= ? AND d.disbursed_at <= ?
GROUP BY DATE_FORMAT(d.disbursed_at, '%Y-%m'), la.branch_code, la.branch_name
ORDER BY disbursement_period DESC, la.branch_code;
```

---

### Report 4: Overdue Reviews
- **Business Purpose**: Identifies delayed underwriting decisions where applications have remained in `UNDER_REVIEW` for strictly more than 2 calendar days.
- **Parameter**: `?` (Business Date, e.g. `CURRENT_DATE()` or simulated date).
- **SQL Implementation**:
```sql
SELECT 
    la.application_id,
    la.application_number,
    c.full_name AS customer_name,
    c.mobile_number,
    la.product_type,
    la.requested_amount,
    la.submitted_at,
    DATEDIFF(?, DATE(la.submitted_at)) AS days_waiting
FROM LOAN_APPLICATION la
JOIN CUSTOMER c ON la.customer_id = c.customer_id
WHERE la.status = 'UNDER_REVIEW'
  AND DATEDIFF(?, DATE(la.submitted_at)) > 2
ORDER BY days_waiting DESC, la.submitted_at ASC;
```

---

### Report 5: Customers Without Disbursement
- **Business Purpose**: Identifies applicant drop-off: customers who have placed at least one loan application but have zero disbursements across any of their applications.
- **SQL Implementation (Anti-Join via `NOT EXISTS`)**:
```sql
SELECT DISTINCT
    c.customer_id,
    c.customer_code,
    c.full_name AS customer_name,
    c.mobile_number,
    c.city,
    c.state,
    c.created_at AS customer_since
FROM CUSTOMER c
JOIN LOAN_APPLICATION la ON c.customer_id = la.customer_id
WHERE NOT EXISTS (
    SELECT 1 
    FROM LOAN_APPLICATION la2
    JOIN DISBURSEMENT d ON la2.application_id = d.application_id
    WHERE la2.customer_id = c.customer_id
)
ORDER BY c.customer_id ASC;
```

---

### Report 6: Repeat Applicants
- **Business Purpose**: Customer lifetime value analysis returning applicants with more than one loan application, total requested amount, and activity history.
- **SQL Implementation (`GROUP BY` and `HAVING`)**:
```sql
SELECT 
    c.customer_id,
    c.customer_code,
    c.full_name AS customer_name,
    c.mobile_number,
    COUNT(la.application_id) AS total_applications,
    COALESCE(SUM(la.requested_amount), 0.00) AS total_requested_amount,
    MIN(la.created_at) AS first_application_date,
    MAX(la.created_at) AS latest_application_date
FROM CUSTOMER c
JOIN LOAN_APPLICATION la ON c.customer_id = la.customer_id
GROUP BY c.customer_id, c.customer_code, c.full_name, c.mobile_number
HAVING COUNT(la.application_id) > 1
ORDER BY total_applications DESC, total_requested_amount DESC;
```

---

### Database Engine Verification & Integration Status
- All 6 reports use parameterized SQL statements implemented in [ReportDAOImpl.java](file:///Users/namanmittal/Desktop/Java%20Project/src/main/java/com/newgen/loanorigination/dao/ReportDAOImpl.java) with explicit parameter binding (`?`), avoiding any string concatenation.
- Queries are ANSI-SQL / MySQL 8.x compliant and verified compatible with MySQL-compatible in-memory engines.
- Test assertions covering all 6 report query executions are codified in [src/test/java/com/newgen/loanorigination/MySqlIntegrationTest.java](file:///Users/namanmittal/Desktop/Java%20Project/src/test/java/com/newgen/loanorigination/MySqlIntegrationTest.java).
- Live execution against an active MySQL server is pending until the local MySQL daemon (`mysqld`) is started on `localhost:3306`.
