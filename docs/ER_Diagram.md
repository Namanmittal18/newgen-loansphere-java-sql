# Entity-Relationship (ER) Diagram & Schema Specification
## Database: `newgen_loan_origination` (MySQL 8.x)

---

### 1. Conceptual Entity Relationship Model

```mermaid
erDiagram
    CUSTOMER ||--o{ LOAN_APPLICATION : "places"
    CUSTOMER ||--o{ NG_CORE_BANKING : "archived in"
    LOAN_APPLICATION ||--o| DISBURSEMENT : "disbursed via"
    LOAN_APPLICATION ||--o{ APPLICATION_HISTORY : "tracks"
    LOAN_APPLICATION ||--o{ SYSTEM_ALERTS : "generates"
    LOAN_APPLICATION ||--o| NG_CORE_BANKING : "pushed to"

    CUSTOMER {
        BIGINT customer_id PK
        VARCHAR customer_code UK "Format: CUST-YYYYMMDD-XXXX"
        VARCHAR full_name
        DATE date_of_birth
        VARCHAR mobile_number UK "10 digits exactly"
        VARCHAR email
        VARCHAR address_line
        VARCHAR city
        VARCHAR state
        VARCHAR postal_code
        VARCHAR kyc_type "AADHAAR, PAN, PASSPORT"
        VARCHAR kyc_reference
        VARCHAR kyc_status "VERIFIED"
        TIMESTAMP created_at
        TIMESTAMP updated_at
    }

    LOAN_APPLICATION {
        BIGINT application_id PK
        VARCHAR application_number UK "Format: APP-YYYYMMDD-XXXX"
        BIGINT customer_id FK
        VARCHAR product_type "CHECK: PERSONAL, EDUCATION"
        VARCHAR branch_code
        VARCHAR branch_name
        DECIMAL requested_amount "CHECK: > 0"
        INT tenure_months "CHECK: > 0"
        DECIMAL monthly_net_income "CHECK: > 0"
        DECIMAL existing_monthly_obligations "CHECK: >= 0"
        DECIMAL annual_interest_rate
        DECIMAL proposed_emi
        DECIMAL foir_percentage
        BOOLEAN eligible
        TEXT eligibility_reasons
        VARCHAR status "CHECK: Canonical 7 States"
        TIMESTAMP created_at
        TIMESTAMP submitted_at
        TIMESTAMP decided_at
        VARCHAR reviewer_code
        TEXT decision_reason
        DECIMAL approved_amount
        TIMESTAMP approved_at
        TIMESTAMP updated_at
    }

    DISBURSEMENT {
        BIGINT disbursement_id PK
        BIGINT application_id UK, FK "1 disbursement per app"
        VARCHAR disbursement_reference UK "Format: DISB-YYYYMMDD-XXXXXX"
        DECIMAL amount "CHECK: > 0"
        TIMESTAMP disbursed_at
    }

    APPLICATION_HISTORY {
        BIGINT history_id PK
        BIGINT application_id FK
        VARCHAR action "CREATE, SUBMIT, APPROVE, REJECT, DISBURSE, CORE_PUSH"
        VARCHAR old_status
        VARCHAR new_status
        VARCHAR performed_by
        TIMESTAMP performed_at
        TEXT remarks
    }

    SYSTEM_ALERTS {
        BIGINT alert_id PK
        BIGINT application_id FK
        VARCHAR alert_type "REVIEW_OVERDUE"
        TIMESTAMP created_at
    }

    UTILITY_RUN {
        BIGINT run_id PK
        VARCHAR utility_name "OVERDUE_REVIEW_JOB, CORE_DATA_PUSH_JOB"
        TIMESTAMP started_at
        TIMESTAMP completed_at
        VARCHAR result "SUCCESS, FAILURE, IN_PROGRESS"
        INT applications_processed
        INT alerts_created
        TEXT error_message
    }

    NG_CORE_BANKING {
        BIGINT core_banking_id PK
        BIGINT customer_id FK
        BIGINT application_id UK, FK "Idempotent push key"
        VARCHAR customer_name
        VARCHAR address
        DATE date_of_birth
        VARCHAR kyc_details
        DECIMAL approved_amount
        DECIMAL disbursed_amount
        TIMESTAMP disbursed_date
        TIMESTAMP pushed_at
    }
```

---

### 2. Table Specifications & Constraint Details

#### 1. `CUSTOMER`
| Column Name | Data Type | Modifiers | Description |
| :--- | :--- | :--- | :--- |
| `customer_id` | `BIGINT` | `PK, AUTO_INCREMENT` | Surrogate primary key |
| `customer_code`| `VARCHAR(30)` | `NOT NULL, UNIQUE` | Unique reference code |
| `full_name` | `VARCHAR(100)`| `NOT NULL` | Full applicant name |
| `date_of_birth`| `DATE` | `NOT NULL` | DOB (Age $21-60$ verified at submission) |
| `mobile_number`| `VARCHAR(15)` | `NOT NULL, UNIQUE` | 10-digit unique mobile |
| `email` | `VARCHAR(100)`| `NULL` | Contact email |
| `address_line` | `VARCHAR(255)`| `NOT NULL` | Street address for Core Banking push |
| `city` | `VARCHAR(50)` | `NOT NULL` | City |
| `state` | `VARCHAR(50)` | `NOT NULL` | State |
| `postal_code` | `VARCHAR(20)` | `NOT NULL` | PIN code |
| `kyc_type` | `VARCHAR(30)` | `NOT NULL` | Aadhaar, PAN, Passport |
| `kyc_reference`| `VARCHAR(50)` | `NOT NULL` | KYC identification number |
| `kyc_status` | `VARCHAR(20)` | `NOT NULL, DEFAULT 'VERIFIED'` | Verification status |

#### 2. `LOAN_APPLICATION`
- **Foreign Key**: `customer_id` references `CUSTOMER(customer_id)`.
- **Unique Constraint**: `application_number` (`APP-YYYYMMDD-XXXX`).
- **Check Constraints**:
  - `status IN ('DRAFT', 'UNDER_REVIEW', 'CREDIT_APPROVAL', 'REJECTED', 'DISBURSED', 'CORE_BANKING_PUSH', 'EXIT')`
  - `product_type IN ('PERSONAL', 'EDUCATION')`
  - `requested_amount > 0`
  - `tenure_months > 0`
  - `monthly_net_income > 0`
  - `existing_monthly_obligations >= 0`
- **Indexes**:
  - `idx_loan_app_active_check (customer_id, product_type, status)`: Accelerates active application concurrency control.
  - `idx_loan_app_review_ageing (status, submitted_at)`: Optimizes overdue review identification.

#### 3. `DISBURSEMENT`
- **Foreign Key**: `application_id` references `LOAN_APPLICATION(application_id)`.
- **Unique Constraint 1**: `application_id` (`UNIQUE`) — Guarantees exactly one disbursement per loan.
- **Unique Constraint 2**: `disbursement_reference` (`UNIQUE`) — Prevents duplicate transaction references.

#### 4. `SYSTEM_ALERTS`
- **Composite Unique Key**: `uk_app_alert (application_id, alert_type)` — Guarantees that multiple utility runs never produce duplicate alerts for the same loan case.

#### 5. `NG_CORE_BANKING`
- **Unique Key**: `application_id` (`UNIQUE`) — Enforces strict idempotency during core banking sync.
