-- ============================================================================
-- Newgen - Mini Loan Origination Application (LoanSphere)
-- Six Required Parameterized SQL Reports
-- ============================================================================

USE newgen_loan_origination;

-- ----------------------------------------------------------------------------
-- REPORT 1: APPLICATION REGISTER
-- Output: Application number, customer name, product, requested amount,
--         EMI, status, and submission date.
-- Filters: product_type (or ALL), status (or ALL).
-- ----------------------------------------------------------------------------
-- Parameter 1: product_type (pass '' or NULL for all products)
-- Parameter 2: status (pass '' or NULL for all statuses)
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


-- ----------------------------------------------------------------------------
-- REPORT 2: PRODUCT PIPELINE
-- Output A: Application count and sum of requested amounts grouped by product and status.
-- Output B: Application count and sum of requested amounts grouped by branch and product.
-- ----------------------------------------------------------------------------

-- Report 2A: Grouped by Product and Status
SELECT 
    la.product_type,
    la.status,
    COUNT(la.application_id) AS application_count,
    COALESCE(SUM(la.requested_amount), 0.00) AS total_requested_amount
FROM LOAN_APPLICATION la
GROUP BY la.product_type, la.status
ORDER BY la.product_type, la.status;

-- Report 2B: Grouped by Branch and Product
SELECT 
    la.branch_code,
    la.branch_name,
    la.product_type,
    COUNT(la.application_id) AS application_count,
    COALESCE(SUM(la.requested_amount), 0.00) AS total_requested_amount
FROM LOAN_APPLICATION la
GROUP BY la.branch_code, la.branch_name, la.product_type
ORDER BY la.branch_code, la.product_type;


-- ----------------------------------------------------------------------------
-- REPORT 3: DAILY / MONTHLY / QUARTERLY DISBURSEMENT
-- Output: Date or period, count, and total amount for a supplied date range.
--         Includes branch-wise breakdown.
-- Parameters:
--   ? : Start date/time (inclusive)
--   ? : End date/time (inclusive)
-- ----------------------------------------------------------------------------

-- Report 3A: Daily Disbursement
SELECT 
    DATE(d.disbursed_at) AS disbursement_period,
    la.branch_code,
    la.branch_name,
    COUNT(d.disbursement_id) AS disbursement_count,
    COALESCE(SUM(d.amount), 0.00) AS total_disbursed_amount
FROM DISBURSEMENT d
JOIN LOAN_APPLICATION la ON d.application_id = la.application_id
WHERE d.disbursed_at >= ? AND d.disbursed_at <= ?
GROUP BY DATE(d.disbursed_at), la.branch_code, la.branch_name
ORDER BY disbursement_period DESC, la.branch_code;

-- Report 3B: Monthly Disbursement
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

-- Report 3C: Quarterly Disbursement
SELECT 
    CONCAT(YEAR(d.disbursed_at), '-Q', QUARTER(d.disbursed_at)) AS disbursement_period,
    la.branch_code,
    la.branch_name,
    COUNT(d.disbursement_id) AS disbursement_count,
    COALESCE(SUM(d.amount), 0.00) AS total_disbursed_amount
FROM DISBURSEMENT d
JOIN LOAN_APPLICATION la ON d.application_id = la.application_id
WHERE d.disbursed_at >= ? AND d.disbursed_at <= ?
GROUP BY YEAR(d.disbursed_at), QUARTER(d.disbursed_at), la.branch_code, la.branch_name
ORDER BY disbursement_period DESC, la.branch_code;


-- ----------------------------------------------------------------------------
-- REPORT 4: OVERDUE REVIEWS
-- Output: Current UNDER_REVIEW applications older than 2 calendar days,
--         including customer, amount, submission date, and days waiting.
-- Parameter:
--   ? : Current Business Date (e.g. '2026-11-23' or CURRENT_DATE())
-- ----------------------------------------------------------------------------
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


-- ----------------------------------------------------------------------------
-- REPORT 5: CUSTOMERS WITHOUT DISBURSEMENT
-- Output: Customers who have at least one application but have no disbursement
--         across ANY of their applications. Return each customer once.
-- ----------------------------------------------------------------------------
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


-- ----------------------------------------------------------------------------
-- REPORT 6: REPEAT APPLICANTS
-- Output: Customers with more than one application, including customer name,
--         application count, and total requested amount.
-- Uses GROUP BY and HAVING.
-- ----------------------------------------------------------------------------
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
