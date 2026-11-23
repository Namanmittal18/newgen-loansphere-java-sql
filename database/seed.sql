-- ============================================================================
-- Newgen - Mini Loan Origination Application (LoanSphere)
-- Demonstration Seed Data (Realistic Fictional Records)
-- ============================================================================

USE newgen_loan_origination;

-- Clear tables in reverse dependency order
DELETE FROM NG_CORE_BANKING;
DELETE FROM SYSTEM_ALERTS;
DELETE FROM APPLICATION_HISTORY;
DELETE FROM DISBURSEMENT;
DELETE FROM UTILITY_RUN;
DELETE FROM LOAN_APPLICATION;
DELETE FROM CUSTOMER;

-- ----------------------------------------------------------------------------
-- 1. SEED CUSTOMERS
-- ----------------------------------------------------------------------------
-- Customer 1: Aarav Sharma, 32 years old, Delhi
INSERT INTO CUSTOMER (customer_id, customer_code, full_name, date_of_birth, mobile_number, email, address_line, city, state, postal_code, kyc_type, kyc_reference, kyc_status, created_at)
VALUES (1, 'CUST-20260101-0001', 'Aarav Sharma', '1994-05-15', '9876543210', 'aarav.sharma@example.com', 'Flat 402, Lotus Heights, Sector 62', 'Noida', 'Uttar Pradesh', '201309', 'AADHAAR', 'AADHAAR-9821-4321-0001', 'VERIFIED', '2026-11-15 10:00:00');

-- Customer 2: Priya Patel, 28 years old, Mumbai
INSERT INTO CUSTOMER (customer_id, customer_code, full_name, date_of_birth, mobile_number, email, address_line, city, state, postal_code, kyc_type, kyc_reference, kyc_status, created_at)
VALUES (2, 'CUST-20260101-0002', 'Priya Patel', '1998-08-22', '9876543211', 'priya.patel@example.com', 'B-14, Shanti Niketan, Andheri East', 'Mumbai', 'Maharashtra', '400069', 'PAN', 'PAN-ABCDE1234F', 'VERIFIED', '2026-11-16 11:30:00');

-- Customer 3: Rohan Mehta, 45 years old, Bengaluru (Repeat applicant)
INSERT INTO CUSTOMER (customer_id, customer_code, full_name, date_of_birth, mobile_number, email, address_line, city, state, postal_code, kyc_type, kyc_reference, kyc_status, created_at)
VALUES (3, 'CUST-20260101-0003', 'Rohan Mehta', '1981-03-10', '9876543212', 'rohan.mehta@example.com', '74/1, 4th Cross, Indiranagar', 'Bengaluru', 'Karnataka', '560038', 'PASSPORT', 'PASS-Z8901234', 'VERIFIED', '2026-11-17 09:15:00');

-- Customer 4: Ananya Iyer, 24 years old, Chennai (Education applicant)
INSERT INTO CUSTOMER (customer_id, customer_code, full_name, date_of_birth, mobile_number, email, address_line, city, state, postal_code, kyc_type, kyc_reference, kyc_status, created_at)
VALUES (4, 'CUST-20260101-0004', 'Ananya Iyer', '2002-11-05', '9876543213', 'ananya.iyer@example.com', 'New Door 18, TTK Road, Alwarpet', 'Chennai', 'Tamil Nadu', '600018', 'AADHAAR', 'AADHAAR-4512-7890-0004', 'VERIFIED', '2026-11-18 14:00:00');

-- Customer 5: Vikram Singh, 38 years old, Pune (Disbursed + Exited)
INSERT INTO CUSTOMER (customer_id, customer_code, full_name, date_of_birth, mobile_number, email, address_line, city, state, postal_code, kyc_type, kyc_reference, kyc_status, created_at)
VALUES (5, 'CUST-20260101-0005', 'Vikram Singh', '1988-02-28', '9876543214', 'vikram.singh@example.com', 'Row House 12, Baner Hills', 'Pune', 'Maharashtra', '411045', 'AADHAAR', 'AADHAAR-6712-3490-0005', 'VERIFIED', '2026-11-10 10:00:00');

-- Customer 6: Sneha Verma, 22 years old, Hyderabad (Under Review, Overdue > 2 days)
INSERT INTO CUSTOMER (customer_id, customer_code, full_name, date_of_birth, mobile_number, email, address_line, city, state, postal_code, kyc_type, kyc_reference, kyc_status, created_at)
VALUES (6, 'CUST-20260101-0006', 'Sneha Verma', '2004-01-19', '9876543215', 'sneha.verma@example.com', 'Plot 88, Madhapur, Cyber City', 'Hyderabad', 'Telangana', '500081', 'PAN', 'PAN-XYZPQ7890M', 'VERIFIED', '2026-11-19 16:45:00');

-- Customer 7: Rajesh Gupta, 52 years old, Jaipur (Draft application)
INSERT INTO CUSTOMER (customer_id, customer_code, full_name, date_of_birth, mobile_number, email, address_line, city, state, postal_code, kyc_type, kyc_reference, kyc_status, created_at)
VALUES (7, 'CUST-20260101-0007', 'Rajesh Gupta', '1974-07-04', '9876543216', 'rajesh.gupta@example.com', 'A-33, Malviya Nagar', 'Jaipur', 'Rajasthan', '302017', 'AADHAAR', 'AADHAAR-8901-2345-0007', 'VERIFIED', '2026-11-21 11:20:00');

-- Customer 8: Neha Reddy, 30 years old, Kolkata (Rejected application)
INSERT INTO CUSTOMER (customer_id, customer_code, full_name, date_of_birth, mobile_number, email, address_line, city, state, postal_code, kyc_type, kyc_reference, kyc_status, created_at)
VALUES (8, 'CUST-20260101-0008', 'Neha Reddy', '1996-09-12', '9876543217', 'neha.reddy@example.com', '52A, Park Street', 'Kolkata', 'West Bengal', '700016', 'PAN', 'PAN-KLMNO4567R', 'VERIFIED', '2026-11-14 12:00:00');

-- ----------------------------------------------------------------------------
-- 2. SEED LOAN APPLICATIONS
-- ----------------------------------------------------------------------------

-- App 1: Customer 1 (Aarav Sharma), PERSONAL, UNDER_REVIEW (recent, exactly 1 day old)
INSERT INTO LOAN_APPLICATION (
    application_id, application_number, customer_id, product_type, branch_code, branch_name,
    requested_amount, tenure_months, monthly_net_income, existing_monthly_obligations,
    annual_interest_rate, proposed_emi, foir_percentage, eligible, eligibility_reasons,
    status, created_at, submitted_at
) VALUES (
    1, 'APP-20261122-0001', 1, 'PERSONAL', 'BR001', 'Connaught Place, New Delhi',
    100000.00, 12, 30000.00, 5000.00,
    12.00, 8884.88, 46.28, TRUE, 'All eligibility checks passed successfully.',
    'UNDER_REVIEW', '2026-11-22 09:30:00', '2026-11-22 10:00:00'
);

-- App 2: Customer 2 (Priya Patel), PERSONAL, CREDIT_APPROVAL (Reviewer approved, ready to disburse)
INSERT INTO LOAN_APPLICATION (
    application_id, application_number, customer_id, product_type, branch_code, branch_name,
    requested_amount, tenure_months, monthly_net_income, existing_monthly_obligations,
    annual_interest_rate, proposed_emi, foir_percentage, eligible, eligibility_reasons,
    status, created_at, submitted_at, decided_at, reviewer_code, decision_reason, approved_amount, approved_at
) VALUES (
    2, 'APP-20261121-0002', 2, 'PERSONAL', 'BR002', 'Bandra Kurla Complex, Mumbai',
    200000.00, 24, 60000.00, 10000.00,
    12.00, 9414.69, 32.36, TRUE, 'All eligibility checks passed successfully.',
    'CREDIT_APPROVAL', '2026-11-21 11:00:00', '2026-11-21 11:30:00', '2026-11-21 15:45:00', 'REV-101', 'Credit profile satisfactory. Income documents verified.', 200000.00, '2026-11-21 15:45:00'
);

-- App 3: Customer 3 (Rohan Mehta - App 1), PERSONAL, EXIT (Completed cycle, Disbursed & Pushed)
INSERT INTO LOAN_APPLICATION (
    application_id, application_number, customer_id, product_type, branch_code, branch_name,
    requested_amount, tenure_months, monthly_net_income, existing_monthly_obligations,
    annual_interest_rate, proposed_emi, foir_percentage, eligible, eligibility_reasons,
    status, created_at, submitted_at, decided_at, reviewer_code, decision_reason, approved_amount, approved_at
) VALUES (
    3, 'APP-20261110-0003', 3, 'PERSONAL', 'BR003', 'MG Road, Bengaluru',
    300000.00, 36, 85000.00, 15000.00,
    12.00, 9964.29, 29.37, TRUE, 'All eligibility checks passed successfully.',
    'EXIT', '2026-11-10 10:00:00', '2026-11-10 10:30:00', '2026-11-11 14:00:00', 'REV-102', 'Approved after credit evaluation.', 300000.00, '2026-11-11 14:00:00'
);

-- App 4: Customer 3 (Rohan Mehta - App 2, Repeat Applicant), EDUCATION, DISBURSED (Awaiting CoreDataPush)
INSERT INTO LOAN_APPLICATION (
    application_id, application_number, customer_id, product_type, branch_code, branch_name,
    requested_amount, tenure_months, monthly_net_income, existing_monthly_obligations,
    annual_interest_rate, proposed_emi, foir_percentage, eligible, eligibility_reasons,
    status, created_at, submitted_at, decided_at, reviewer_code, decision_reason, approved_amount, approved_at
) VALUES (
    4, 'APP-20261118-0004', 3, 'EDUCATION', 'BR003', 'MG Road, Bengaluru',
    500000.00, 48, 85000.00, 15000.00,
    10.00, 12681.29, 32.57, TRUE, 'All eligibility checks passed successfully.',
    'DISBURSED', '2026-11-18 10:00:00', '2026-11-18 10:30:00', '2026-11-19 16:00:00', 'REV-101', 'University admission confirmed and collateral valid.', 500000.00, '2026-11-19 16:00:00'
);

-- App 5: Customer 4 (Ananya Iyer), EDUCATION, UNDER_REVIEW (recent)
INSERT INTO LOAN_APPLICATION (
    application_id, application_number, customer_id, product_type, branch_code, branch_name,
    requested_amount, tenure_months, monthly_net_income, existing_monthly_obligations,
    annual_interest_rate, proposed_emi, foir_percentage, eligible, eligibility_reasons,
    status, created_at, submitted_at
) VALUES (
    5, 'APP-20261122-0005', 4, 'EDUCATION', 'BR004', 'Anna Salai, Chennai',
    400000.00, 36, 40000.00, 3000.00,
    10.00, 12906.91, 39.77, TRUE, 'All eligibility checks passed successfully.',
    'UNDER_REVIEW', '2026-11-22 11:00:00', '2026-11-22 11:30:00'
);

-- App 6: Customer 5 (Vikram Singh), PERSONAL, EXIT (Completed full cycle)
INSERT INTO LOAN_APPLICATION (
    application_id, application_number, customer_id, product_type, branch_code, branch_name,
    requested_amount, tenure_months, monthly_net_income, existing_monthly_obligations,
    annual_interest_rate, proposed_emi, foir_percentage, eligible, eligibility_reasons,
    status, created_at, submitted_at, decided_at, reviewer_code, decision_reason, approved_amount, approved_at
) VALUES (
    6, 'APP-20261112-0006', 5, 'PERSONAL', 'BR005', 'Shivaji Nagar, Pune',
    150000.00, 18, 55000.00, 5000.00,
    12.00, 9148.91, 25.73, TRUE, 'All eligibility checks passed successfully.',
    'EXIT', '2026-11-12 09:00:00', '2026-11-12 09:30:00', '2026-11-13 11:00:00', 'REV-103', 'Approved.', 150000.00, '2026-11-13 11:00:00'
);

-- App 7: Customer 6 (Sneha Verma), EDUCATION, UNDER_REVIEW (Submitted on 2026-11-19, 4 days old -> OVERDUE!)
INSERT INTO LOAN_APPLICATION (
    application_id, application_number, customer_id, product_type, branch_code, branch_name,
    requested_amount, tenure_months, monthly_net_income, existing_monthly_obligations,
    annual_interest_rate, proposed_emi, foir_percentage, eligible, eligibility_reasons,
    status, created_at, submitted_at
) VALUES (
    7, 'APP-20261119-0007', 6, 'EDUCATION', 'BR006', 'Hitec City, Hyderabad',
    600000.00, 60, 50000.00, 8000.00,
    10.00, 12748.23, 41.50, TRUE, 'All eligibility checks passed successfully.',
    'UNDER_REVIEW', '2026-11-19 14:00:00', '2026-11-19 14:30:00'
);

-- App 8: Customer 7 (Rajesh Gupta), PERSONAL, DRAFT
INSERT INTO LOAN_APPLICATION (
    application_id, application_number, customer_id, product_type, branch_code, branch_name,
    requested_amount, tenure_months, monthly_net_income, existing_monthly_obligations,
    annual_interest_rate, proposed_emi, foir_percentage, eligible, eligibility_reasons,
    status, created_at
) VALUES (
    8, 'APP-20261121-0008', 7, 'PERSONAL', 'BR007', 'MI Road, Jaipur',
    250000.00, 24, 45000.00, 12000.00,
    12.00, 11768.36, 52.82, FALSE, NULL,
    'DRAFT', '2026-11-21 16:00:00'
);

-- App 9: Customer 8 (Neha Reddy), PERSONAL, REJECTED (FOIR > 50% on submission)
INSERT INTO LOAN_APPLICATION (
    application_id, application_number, customer_id, product_type, branch_code, branch_name,
    requested_amount, tenure_months, monthly_net_income, existing_monthly_obligations,
    annual_interest_rate, proposed_emi, foir_percentage, eligible, eligibility_reasons,
    status, created_at, submitted_at, decided_at, reviewer_code, decision_reason
) VALUES (
    9, 'APP-20261115-0009', 8, 'PERSONAL', 'BR008', 'Salt Lake, Kolkata',
    100000.00, 12, 30000.00, 7000.00,
    12.00, 8884.88, 52.95, FALSE, 'RULE_FAILURE: FOIR 52.95% exceeds maximum threshold of 50.00%',
    'REJECTED', '2026-11-15 11:00:00', '2026-11-15 11:30:00', '2026-11-15 11:30:00', 'SYSTEM_RULE_ENGINE', 'Automatic rejection due to affordability policy failure.'
);

-- ----------------------------------------------------------------------------
-- 3. SEED DISBURSEMENTS
-- ----------------------------------------------------------------------------
-- Disbursement for App 3 (Customer 3)
INSERT INTO DISBURSEMENT (disbursement_id, application_id, disbursement_reference, amount, disbursed_at)
VALUES (1, 3, 'DISB-20261112-000101', 300000.00, '2026-11-12 11:30:00');

-- Disbursement for App 4 (Customer 3)
INSERT INTO DISBURSEMENT (disbursement_id, application_id, disbursement_reference, amount, disbursed_at)
VALUES (2, 4, 'DISB-20261120-000102', 500000.00, '2026-11-20 10:45:00');

-- Disbursement for App 6 (Customer 5)
INSERT INTO DISBURSEMENT (disbursement_id, application_id, disbursement_reference, amount, disbursed_at)
VALUES (3, 6, 'DISB-20261114-000103', 150000.00, '2026-11-14 15:20:00');

-- ----------------------------------------------------------------------------
-- 4. SEED APPLICATION HISTORY
-- ----------------------------------------------------------------------------
INSERT INTO APPLICATION_HISTORY (application_id, action, old_status, new_status, performed_by, performed_at, remarks)
VALUES 
(1, 'CREATE', NULL, 'DRAFT', 'CUSTOMER', '2026-11-22 09:30:00', 'Application drafted online.'),
(1, 'SUBMIT', 'DRAFT', 'UNDER_REVIEW', 'CUSTOMER', '2026-11-22 10:00:00', 'Passed automated eligibility checks with FOIR 46.28%.'),

(2, 'CREATE', NULL, 'DRAFT', 'CUSTOMER', '2026-11-21 11:00:00', 'Application drafted.'),
(2, 'SUBMIT', 'DRAFT', 'UNDER_REVIEW', 'CUSTOMER', '2026-11-21 11:30:00', 'Passed automated eligibility checks with FOIR 32.36%.'),
(2, 'APPROVE', 'UNDER_REVIEW', 'CREDIT_APPROVAL', 'REV-101', '2026-11-21 15:45:00', 'Approved after credit evaluation.'),

(3, 'CREATE', NULL, 'DRAFT', 'CUSTOMER', '2026-11-10 10:00:00', 'Application drafted.'),
(3, 'SUBMIT', 'DRAFT', 'UNDER_REVIEW', 'CUSTOMER', '2026-11-10 10:30:00', 'Passed automated eligibility checks.'),
(3, 'APPROVE', 'UNDER_REVIEW', 'CREDIT_APPROVAL', 'REV-102', '2026-11-11 14:00:00', 'Approved by credit officer.'),
(3, 'DISBURSE', 'CREDIT_APPROVAL', 'DISBURSED', 'OPS-DISB', '2026-11-12 11:30:00', 'Full amount disbursed with ref DISB-20261112-000101.'),
(3, 'CORE_PUSH', 'DISBURSED', 'EXIT', 'CORE_DATA_PUSH_JOB', '2026-11-12 12:00:00', 'Record synced to NG_CORE_BANKING. Case marked EXIT.'),

(4, 'CREATE', NULL, 'DRAFT', 'CUSTOMER', '2026-11-18 10:00:00', 'Application drafted.'),
(4, 'SUBMIT', 'DRAFT', 'UNDER_REVIEW', 'CUSTOMER', '2026-11-18 10:30:00', 'Passed automated eligibility checks.'),
(4, 'APPROVE', 'UNDER_REVIEW', 'CREDIT_APPROVAL', 'REV-101', '2026-11-19 16:00:00', 'Approved by credit officer.'),
(4, 'DISBURSE', 'CREDIT_APPROVAL', 'DISBURSED', 'OPS-DISB', '2026-11-20 10:45:00', 'Disbursed with ref DISB-20261120-000102.'),

(7, 'CREATE', NULL, 'DRAFT', 'CUSTOMER', '2026-11-19 14:00:00', 'Application drafted.'),
(7, 'SUBMIT', 'DRAFT', 'UNDER_REVIEW', 'CUSTOMER', '2026-11-19 14:30:00', 'Passed automated eligibility checks.'),

(9, 'CREATE', NULL, 'DRAFT', 'CUSTOMER', '2026-11-15 11:00:00', 'Application drafted.'),
(9, 'SUBMIT_REJECTED', 'DRAFT', 'REJECTED', 'SYSTEM', '2026-11-15 11:30:00', 'System rejection: FOIR 52.95% exceeds 50.00% threshold.');

-- ----------------------------------------------------------------------------
-- 5. SEED SYSTEM ALERTS
-- ----------------------------------------------------------------------------
-- Alert for App 7 which has been in UNDER_REVIEW for 4 calendar days
INSERT INTO SYSTEM_ALERTS (alert_id, application_id, alert_type, created_at)
VALUES (1, 7, 'REVIEW_OVERDUE', '2026-11-22 00:05:00');

-- ----------------------------------------------------------------------------
-- 6. SEED UTILITY RUN
-- ----------------------------------------------------------------------------
INSERT INTO UTILITY_RUN (run_id, utility_name, started_at, completed_at, result, applications_processed, alerts_created, error_message)
VALUES 
(1, 'OVERDUE_REVIEW_JOB', '2026-11-22 00:00:00', '2026-11-22 00:05:02', 'SUCCESS', 3, 1, NULL),
(2, 'CORE_DATA_PUSH_JOB', '2026-11-12 12:00:00', '2026-11-12 12:00:01', 'SUCCESS', 1, 0, NULL);

-- ----------------------------------------------------------------------------
-- 7. SEED NG_CORE_BANKING
-- ----------------------------------------------------------------------------
-- Core banking entries for App 3 and App 6 which reached EXIT stage
INSERT INTO NG_CORE_BANKING (
    core_banking_id, customer_id, application_id, customer_name, address,
    date_of_birth, kyc_details, approved_amount, disbursed_amount, disbursed_date, pushed_at
) VALUES 
(1, 3, 3, 'Rohan Mehta', '74/1, 4th Cross, Indiranagar, Bengaluru, Karnataka, 560038', '1981-03-10', 'PASSPORT: PASS-Z8901234', 300000.00, 300000.00, '2026-11-12 11:30:00', '2026-11-12 12:00:00'),
(2, 5, 6, 'Vikram Singh', 'Row House 12, Baner Hills, Pune, Maharashtra, 411045', '1988-02-28', 'AADHAAR: AADHAAR-6712-3490-0005', 150000.00, 150000.00, '2026-11-14 15:20:00', '2026-11-14 15:30:00');
