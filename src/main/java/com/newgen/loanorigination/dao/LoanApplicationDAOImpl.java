package com.newgen.loanorigination.dao;

import com.newgen.loanorigination.exception.DatabaseOperationException;
import com.newgen.loanorigination.model.LoanApplication;
import com.newgen.loanorigination.model.enums.ApplicationStatus;
import com.newgen.loanorigination.model.enums.ProductType;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public class LoanApplicationDAOImpl implements LoanApplicationDAO {

    private final JdbcTemplate jdbcTemplate;

    public LoanApplicationDAOImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final RowMapper<LoanApplication> LOAN_APPLICATION_ROW_MAPPER = (rs, rowNum) -> {
        LoanApplication app = new LoanApplication();
        app.setApplicationId(rs.getLong("application_id"));
        app.setApplicationNumber(rs.getString("application_number"));
        app.setCustomerId(rs.getLong("customer_id"));
        String prod = rs.getString("product_type");
        app.setProductType(prod != null ? ProductType.valueOf(prod) : null);
        app.setBranchCode(rs.getString("branch_code"));
        app.setBranchName(rs.getString("branch_name"));
        app.setRequestedAmount(rs.getBigDecimal("requested_amount"));
        app.setTenureMonths(rs.getInt("tenure_months"));
        app.setMonthlyNetIncome(rs.getBigDecimal("monthly_net_income"));
        app.setExistingMonthlyObligations(rs.getBigDecimal("existing_monthly_obligations"));
        app.setAnnualInterestRate(rs.getBigDecimal("annual_interest_rate"));
        app.setProposedEmi(rs.getBigDecimal("proposed_emi"));
        app.setFoirPercentage(rs.getBigDecimal("foir_percentage"));
        app.setEligible(rs.getBoolean("eligible"));
        app.setEligibilityReasons(rs.getString("eligibility_reasons"));
        String st = rs.getString("status");
        app.setStatus(st != null ? ApplicationStatus.valueOf(st) : null);

        Timestamp cat = rs.getTimestamp("created_at");
        app.setCreatedAt(cat != null ? cat.toLocalDateTime() : null);
        Timestamp sub = rs.getTimestamp("submitted_at");
        app.setSubmittedAt(sub != null ? sub.toLocalDateTime() : null);
        Timestamp dec = rs.getTimestamp("decided_at");
        app.setDecidedAt(dec != null ? dec.toLocalDateTime() : null);

        app.setReviewerCode(rs.getString("reviewer_code"));
        app.setDecisionReason(rs.getString("decision_reason"));
        app.setApprovedAmount(rs.getBigDecimal("approved_amount"));

        Timestamp apprv = rs.getTimestamp("approved_at");
        app.setApprovedAt(apprv != null ? apprv.toLocalDateTime() : null);
        Timestamp uat = rs.getTimestamp("updated_at");
        app.setUpdatedAt(uat != null ? uat.toLocalDateTime() : null);

        return app;
    };

    @Override
    public LoanApplication save(LoanApplication application) {
        String sql = "INSERT INTO LOAN_APPLICATION (application_number, customer_id, product_type, branch_code, " +
                "branch_name, requested_amount, tenure_months, monthly_net_income, existing_monthly_obligations, " +
                "annual_interest_rate, proposed_emi, foir_percentage, eligible, eligibility_reasons, status, " +
                "created_at, submitted_at, decided_at, reviewer_code, decision_reason, approved_amount, approved_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, application.getApplicationNumber());
                ps.setLong(2, application.getCustomerId());
                ps.setString(3, application.getProductType().name());
                ps.setString(4, application.getBranchCode());
                ps.setString(5, application.getBranchName());
                ps.setBigDecimal(6, application.getRequestedAmount());
                ps.setInt(7, application.getTenureMonths());
                ps.setBigDecimal(8, application.getMonthlyNetIncome());
                ps.setBigDecimal(9, application.getExistingMonthlyObligations());
                ps.setBigDecimal(10, application.getAnnualInterestRate());
                ps.setBigDecimal(11, application.getProposedEmi());
                ps.setBigDecimal(12, application.getFoirPercentage());
                ps.setBoolean(13, application.getEligible() != null && application.getEligible());
                ps.setString(14, application.getEligibilityReasons());
                ps.setString(15, application.getStatus().name());
                ps.setTimestamp(16, application.getCreatedAt() != null ?
                        Timestamp.valueOf(application.getCreatedAt()) : new Timestamp(System.currentTimeMillis()));
                ps.setTimestamp(17, application.getSubmittedAt() != null ?
                        Timestamp.valueOf(application.getSubmittedAt()) : null);
                ps.setTimestamp(18, application.getDecidedAt() != null ?
                        Timestamp.valueOf(application.getDecidedAt()) : null);
                ps.setString(19, application.getReviewerCode());
                ps.setString(20, application.getDecisionReason());
                ps.setBigDecimal(21, application.getApprovedAmount());
                ps.setTimestamp(22, application.getApprovedAt() != null ?
                        Timestamp.valueOf(application.getApprovedAt()) : null);
                return ps;
            }, keyHolder);

            if (keyHolder.getKey() != null) {
                application.setApplicationId(keyHolder.getKey().longValue());
            }
            return application;
        } catch (DataAccessException e) {
            throw new DatabaseOperationException("Failed to save loan application: " + e.getMessage(), e);
        }
    }

    @Override
    public void update(LoanApplication application) {
        String sql = "UPDATE LOAN_APPLICATION SET product_type = ?, branch_code = ?, branch_name = ?, " +
                "requested_amount = ?, tenure_months = ?, monthly_net_income = ?, existing_monthly_obligations = ?, " +
                "annual_interest_rate = ?, proposed_emi = ?, foir_percentage = ?, eligible = ?, " +
                "eligibility_reasons = ?, status = ?, submitted_at = ?, decided_at = ?, reviewer_code = ?, " +
                "decision_reason = ?, approved_amount = ?, approved_at = ? WHERE application_id = ?";
        try {
            jdbcTemplate.update(sql,
                    application.getProductType().name(),
                    application.getBranchCode(),
                    application.getBranchName(),
                    application.getRequestedAmount(),
                    application.getTenureMonths(),
                    application.getMonthlyNetIncome(),
                    application.getExistingMonthlyObligations(),
                    application.getAnnualInterestRate(),
                    application.getProposedEmi(),
                    application.getFoirPercentage(),
                    application.getEligible(),
                    application.getEligibilityReasons(),
                    application.getStatus().name(),
                    application.getSubmittedAt() != null ? Timestamp.valueOf(application.getSubmittedAt()) : null,
                    application.getDecidedAt() != null ? Timestamp.valueOf(application.getDecidedAt()) : null,
                    application.getReviewerCode(),
                    application.getDecisionReason(),
                    application.getApprovedAmount(),
                    application.getApprovedAt() != null ? Timestamp.valueOf(application.getApprovedAt()) : null,
                    application.getApplicationId());
        } catch (DataAccessException e) {
            throw new DatabaseOperationException("Failed to update loan application: " + e.getMessage(), e);
        }
    }

    @Override
    public void updateStatus(Long applicationId, ApplicationStatus status) {
        String sql = "UPDATE LOAN_APPLICATION SET status = ? WHERE application_id = ?";
        try {
            jdbcTemplate.update(sql, status.name(), applicationId);
        } catch (DataAccessException e) {
            throw new DatabaseOperationException("Failed to update application status: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<LoanApplication> findById(Long applicationId) {
        String sql = "SELECT * FROM LOAN_APPLICATION WHERE application_id = ?";
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, LOAN_APPLICATION_ROW_MAPPER, applicationId));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        } catch (DataAccessException e) {
            throw new DatabaseOperationException("Failed to query application by ID: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<LoanApplication> findByApplicationNumber(String applicationNumber) {
        String sql = "SELECT * FROM LOAN_APPLICATION WHERE application_number = ?";
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, LOAN_APPLICATION_ROW_MAPPER, applicationNumber));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        } catch (DataAccessException e) {
            throw new DatabaseOperationException("Failed to query application by number: " + e.getMessage(), e);
        }
    }

    @Override
    public List<LoanApplication> findByCustomerId(Long customerId) {
        String sql = "SELECT * FROM LOAN_APPLICATION WHERE customer_id = ? ORDER BY created_at DESC";
        try {
            return jdbcTemplate.query(sql, LOAN_APPLICATION_ROW_MAPPER, customerId);
        } catch (DataAccessException e) {
            throw new DatabaseOperationException("Failed to query applications for customer: " + e.getMessage(), e);
        }
    }

    @Override
    public List<LoanApplication> findByStatus(ApplicationStatus status) {
        String sql = "SELECT * FROM LOAN_APPLICATION WHERE status = ? ORDER BY created_at DESC";
        try {
            return jdbcTemplate.query(sql, LOAN_APPLICATION_ROW_MAPPER, status.name());
        } catch (DataAccessException e) {
            throw new DatabaseOperationException("Failed to query applications by status: " + e.getMessage(), e);
        }
    }

    @Override
    public List<LoanApplication> findAll() {
        String sql = "SELECT * FROM LOAN_APPLICATION ORDER BY created_at DESC";
        try {
            return jdbcTemplate.query(sql, LOAN_APPLICATION_ROW_MAPPER);
        } catch (DataAccessException e) {
            throw new DatabaseOperationException("Failed to list all applications: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean hasActiveApplication(Long customerId, ProductType productType) {
        String sql = "SELECT COUNT(*) FROM LOAN_APPLICATION WHERE customer_id = ? AND product_type = ? " +
                "AND status IN ('DRAFT', 'UNDER_REVIEW', 'CREDIT_APPROVAL')";
        try {
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, customerId, productType.name());
            return count != null && count > 0;
        } catch (DataAccessException e) {
            throw new DatabaseOperationException("Failed to check active application: " + e.getMessage(), e);
        }
    }

    @Override
    public List<LoanApplication> findOverdueUnderReview(LocalDate businessDate, int thresholdDays) {
        LocalDate effDate = businessDate != null ? businessDate : LocalDate.now();
        // Overdue strictly when DATEDIFF > thresholdDays (e.g. > 2 calendar days)
        String sql = "SELECT * FROM LOAN_APPLICATION WHERE status = 'UNDER_REVIEW' " +
                "AND DATEDIFF(?, DATE(submitted_at)) > ? ORDER BY submitted_at ASC";
        try {
            return jdbcTemplate.query(sql, LOAN_APPLICATION_ROW_MAPPER, Date.valueOf(effDate), thresholdDays);
        } catch (DataAccessException e) {
            throw new DatabaseOperationException("Failed to find overdue applications: " + e.getMessage(), e);
        }
    }

    @Override
    public List<LoanApplication> findDisbursedPendingPush() {
        String sql = "SELECT * FROM LOAN_APPLICATION WHERE status = 'DISBURSED' ORDER BY created_at ASC";
        try {
            return jdbcTemplate.query(sql, LOAN_APPLICATION_ROW_MAPPER);
        } catch (DataAccessException e) {
            throw new DatabaseOperationException("Failed to find disbursed applications: " + e.getMessage(), e);
        }
    }

    @Override
    public long countByStatus(ApplicationStatus status) {
        String sql = "SELECT COUNT(*) FROM LOAN_APPLICATION WHERE status = ?";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, status.name());
        return count != null ? count : 0L;
    }

    @Override
    public long countTotal() {
        String sql = "SELECT COUNT(*) FROM LOAN_APPLICATION";
        Long count = jdbcTemplate.queryForObject(sql, Long.class);
        return count != null ? count : 0L;
    }
}
