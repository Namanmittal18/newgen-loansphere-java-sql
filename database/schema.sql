-- ============================================================================
-- Newgen - Mini Loan Origination Application (LoanSphere)
-- Database Schema Definition (MySQL 8.x)
-- ============================================================================

CREATE DATABASE IF NOT EXISTS newgen_loan_origination;
USE newgen_loan_origination;

-- Drop child tables first in reverse dependency order
DROP TABLE IF EXISTS NG_CORE_BANKING;
DROP TABLE IF EXISTS SYSTEM_ALERTS;
DROP TABLE IF EXISTS APPLICATION_HISTORY;
DROP TABLE IF EXISTS DISBURSEMENT;
DROP TABLE IF EXISTS UTILITY_RUN;
DROP TABLE IF EXISTS LOAN_APPLICATION;
DROP TABLE IF EXISTS CUSTOMER;

-- ----------------------------------------------------------------------------
-- 1. CUSTOMER TABLE
-- ----------------------------------------------------------------------------
CREATE TABLE CUSTOMER (
    customer_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_code VARCHAR(30) NOT NULL UNIQUE,
    full_name VARCHAR(100) NOT NULL,
    date_of_birth DATE NOT NULL,
    mobile_number VARCHAR(15) NOT NULL UNIQUE,
    email VARCHAR(100),
    address_line VARCHAR(255) NOT NULL,
    city VARCHAR(50) NOT NULL,
    state VARCHAR(50) NOT NULL,
    postal_code VARCHAR(20) NOT NULL,
    kyc_type VARCHAR(30) NOT NULL,
    kyc_reference VARCHAR(50) NOT NULL,
    kyc_status VARCHAR(20) NOT NULL DEFAULT 'VERIFIED',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT chk_mobile_length CHECK (CHAR_LENGTH(mobile_number) = 10)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 2. LOAN_APPLICATION TABLE
-- ----------------------------------------------------------------------------
CREATE TABLE LOAN_APPLICATION (
    application_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    application_number VARCHAR(30) NOT NULL UNIQUE,
    customer_id BIGINT NOT NULL,
    product_type VARCHAR(20) NOT NULL,
    branch_code VARCHAR(20) NOT NULL,
    branch_name VARCHAR(100) NOT NULL,
    requested_amount DECIMAL(15, 2) NOT NULL,
    tenure_months INT NOT NULL,
    monthly_net_income DECIMAL(15, 2) NOT NULL,
    existing_monthly_obligations DECIMAL(15, 2) NOT NULL,
    annual_interest_rate DECIMAL(5, 2) NOT NULL,
    proposed_emi DECIMAL(15, 2) NOT NULL,
    foir_percentage DECIMAL(5, 2) NOT NULL,
    eligible BOOLEAN NOT NULL DEFAULT FALSE,
    eligibility_reasons TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    submitted_at TIMESTAMP NULL,
    decided_at TIMESTAMP NULL,
    reviewer_code VARCHAR(50) NULL,
    decision_reason TEXT NULL,
    approved_amount DECIMAL(15, 2) NULL,
    approved_at TIMESTAMP NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_app_customer FOREIGN KEY (customer_id) REFERENCES CUSTOMER (customer_id) ON DELETE RESTRICT,
    CONSTRAINT chk_product_type CHECK (product_type IN ('PERSONAL', 'EDUCATION')),
    CONSTRAINT chk_app_status CHECK (status IN ('DRAFT', 'UNDER_REVIEW', 'CREDIT_APPROVAL', 'REJECTED', 'DISBURSED', 'CORE_BANKING_PUSH', 'EXIT')),
    CONSTRAINT chk_requested_amount CHECK (requested_amount > 0),
    CONSTRAINT chk_tenure CHECK (tenure_months > 0),
    CONSTRAINT chk_income CHECK (monthly_net_income > 0),
    CONSTRAINT chk_obligations CHECK (existing_monthly_obligations >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_loan_app_customer ON LOAN_APPLICATION (customer_id);
CREATE INDEX idx_loan_app_status ON LOAN_APPLICATION (status);
CREATE INDEX idx_loan_app_active_check ON LOAN_APPLICATION (customer_id, product_type, status);
CREATE INDEX idx_loan_app_review_ageing ON LOAN_APPLICATION (status, submitted_at);
CREATE INDEX idx_loan_app_branch ON LOAN_APPLICATION (branch_code, product_type);

-- ----------------------------------------------------------------------------
-- 3. DISBURSEMENT TABLE
-- ----------------------------------------------------------------------------
CREATE TABLE DISBURSEMENT (
    disbursement_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    application_id BIGINT NOT NULL UNIQUE,
    disbursement_reference VARCHAR(50) NOT NULL UNIQUE,
    amount DECIMAL(15, 2) NOT NULL,
    disbursed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_disb_application FOREIGN KEY (application_id) REFERENCES LOAN_APPLICATION (application_id) ON DELETE RESTRICT,
    CONSTRAINT chk_disb_amount CHECK (amount > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_disb_reference ON DISBURSEMENT (disbursement_reference);
CREATE INDEX idx_disb_date ON DISBURSEMENT (disbursed_at);

-- ----------------------------------------------------------------------------
-- 4. APPLICATION_HISTORY TABLE
-- ----------------------------------------------------------------------------
CREATE TABLE APPLICATION_HISTORY (
    history_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    application_id BIGINT NOT NULL,
    action VARCHAR(50) NOT NULL,
    old_status VARCHAR(30) NULL,
    new_status VARCHAR(30) NOT NULL,
    performed_by VARCHAR(50) NOT NULL,
    performed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    remarks TEXT NULL,
    CONSTRAINT fk_hist_application FOREIGN KEY (application_id) REFERENCES LOAN_APPLICATION (application_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_hist_application ON APPLICATION_HISTORY (application_id);
CREATE INDEX idx_hist_timestamp ON APPLICATION_HISTORY (performed_at);

-- ----------------------------------------------------------------------------
-- 5. SYSTEM_ALERTS TABLE
-- ----------------------------------------------------------------------------
CREATE TABLE SYSTEM_ALERTS (
    alert_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    application_id BIGINT NOT NULL,
    alert_type VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_alert_application FOREIGN KEY (application_id) REFERENCES LOAN_APPLICATION (application_id) ON DELETE CASCADE,
    CONSTRAINT uk_app_alert UNIQUE (application_id, alert_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_alert_type ON SYSTEM_ALERTS (alert_type);

-- ----------------------------------------------------------------------------
-- 6. UTILITY_RUN TABLE
-- ----------------------------------------------------------------------------
CREATE TABLE UTILITY_RUN (
    run_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    utility_name VARCHAR(50) NOT NULL,
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP NULL,
    result VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    applications_processed INT NOT NULL DEFAULT 0,
    alerts_created INT NOT NULL DEFAULT 0,
    error_message TEXT NULL,
    CONSTRAINT chk_utility_result CHECK (result IN ('SUCCESS', 'FAILURE', 'IN_PROGRESS'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_utility_name ON UTILITY_RUN (utility_name);
CREATE INDEX idx_utility_started ON UTILITY_RUN (started_at);

-- ----------------------------------------------------------------------------
-- 7. NG_CORE_BANKING TABLE
-- ----------------------------------------------------------------------------
CREATE TABLE NG_CORE_BANKING (
    core_banking_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    application_id BIGINT NOT NULL UNIQUE,
    customer_name VARCHAR(100) NOT NULL,
    address VARCHAR(255) NOT NULL,
    date_of_birth DATE NOT NULL,
    kyc_details VARCHAR(100) NOT NULL,
    approved_amount DECIMAL(15, 2) NOT NULL,
    disbursed_amount DECIMAL(15, 2) NOT NULL,
    disbursed_date TIMESTAMP NOT NULL,
    pushed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_core_customer FOREIGN KEY (customer_id) REFERENCES CUSTOMER (customer_id) ON DELETE RESTRICT,
    CONSTRAINT fk_core_application FOREIGN KEY (application_id) REFERENCES LOAN_APPLICATION (application_id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_core_customer ON NG_CORE_BANKING (customer_id);
CREATE INDEX idx_core_pushed_at ON NG_CORE_BANKING (pushed_at);
