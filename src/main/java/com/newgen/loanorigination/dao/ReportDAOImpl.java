package com.newgen.loanorigination.dao;

import com.newgen.loanorigination.dto.reports.*;
import com.newgen.loanorigination.exception.DatabaseOperationException;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Repository
public class ReportDAOImpl implements ReportDAO {

    private final JdbcTemplate jdbcTemplate;

    public ReportDAOImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<ApplicationRegisterReportDTO> getApplicationRegister(String productType, String status) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT la.application_number, c.full_name AS customer_name, la.product_type, ")
           .append("la.requested_amount, la.proposed_emi, la.status, la.submitted_at ")
           .append("FROM LOAN_APPLICATION la ")
           .append("JOIN CUSTOMER c ON la.customer_id = c.customer_id WHERE 1=1 ");

        List<Object> params = new ArrayList<>();
        if (productType != null && !productType.trim().isEmpty() && !"ALL".equalsIgnoreCase(productType)) {
            sql.append("AND la.product_type = ? ");
            params.add(productType.trim());
        }
        if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status)) {
            sql.append("AND la.status = ? ");
            params.add(status.trim());
        }
        sql.append("ORDER BY la.created_at DESC");

        try {
            return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> {
                ApplicationRegisterReportDTO dto = new ApplicationRegisterReportDTO();
                dto.setApplicationNumber(rs.getString("application_number"));
                dto.setCustomerName(rs.getString("customer_name"));
                dto.setProductType(rs.getString("product_type"));
                dto.setRequestedAmount(rs.getBigDecimal("requested_amount"));
                dto.setProposedEmi(rs.getBigDecimal("proposed_emi"));
                dto.setStatus(rs.getString("status"));
                Timestamp sub = rs.getTimestamp("submitted_at");
                dto.setSubmittedAt(sub != null ? sub.toLocalDateTime() : null);
                return dto;
            }, params.toArray());
        } catch (DataAccessException e) {
            throw new DatabaseOperationException("Failed to generate Application Register Report: " + e.getMessage(), e);
        }
    }

    @Override
    public List<ProductPipelineStatusDTO> getProductPipelineByStatus() {
        String sql = "SELECT la.product_type, la.status, COUNT(la.application_id) AS app_count, " +
                "COALESCE(SUM(la.requested_amount), 0.00) AS total_amount " +
                "FROM LOAN_APPLICATION la " +
                "GROUP BY la.product_type, la.status " +
                "ORDER BY la.product_type, la.status";
        try {
            return jdbcTemplate.query(sql, (rs, rowNum) -> new ProductPipelineStatusDTO(
                    rs.getString("product_type"),
                    rs.getString("status"),
                    rs.getLong("app_count"),
                    rs.getBigDecimal("total_amount")
            ));
        } catch (DataAccessException e) {
            throw new DatabaseOperationException("Failed to generate Product Pipeline by Status: " + e.getMessage(), e);
        }
    }

    @Override
    public List<ProductPipelineBranchDTO> getProductPipelineByBranch() {
        String sql = "SELECT la.branch_code, la.branch_name, la.product_type, COUNT(la.application_id) AS app_count, " +
                "COALESCE(SUM(la.requested_amount), 0.00) AS total_amount " +
                "FROM LOAN_APPLICATION la " +
                "GROUP BY la.branch_code, la.branch_name, la.product_type " +
                "ORDER BY la.branch_code, la.product_type";
        try {
            return jdbcTemplate.query(sql, (rs, rowNum) -> new ProductPipelineBranchDTO(
                    rs.getString("branch_code"),
                    rs.getString("branch_name"),
                    rs.getString("product_type"),
                    rs.getLong("app_count"),
                    rs.getBigDecimal("total_amount")
            ));
        } catch (DataAccessException e) {
            throw new DatabaseOperationException("Failed to generate Product Pipeline by Branch: " + e.getMessage(), e);
        }
    }

    @Override
    public List<DisbursementSummaryDTO> getDisbursementSummary(LocalDate startDate, LocalDate endDate, String periodType) {
        LocalDate start = (startDate != null) ? startDate : LocalDate.now().minusYears(1);
        LocalDate end = (endDate != null) ? endDate : LocalDate.now().plusDays(1);

        Timestamp startTs = Timestamp.valueOf(LocalDateTime.of(start, LocalTime.MIN));
        Timestamp endTs = Timestamp.valueOf(LocalDateTime.of(end, LocalTime.MAX));

        String dateExpr;
        if ("MONTHLY".equalsIgnoreCase(periodType)) {
            dateExpr = "SUBSTRING(CAST(d.disbursed_at AS VARCHAR(25)), 1, 7)";
        } else if ("QUARTERLY".equalsIgnoreCase(periodType)) {
            dateExpr = "CONCAT(SUBSTRING(CAST(d.disbursed_at AS VARCHAR(25)), 1, 4), '-Q', CAST((CAST(SUBSTRING(CAST(d.disbursed_at AS VARCHAR(25)), 6, 2) AS INT) - 1) / 3 + 1 AS VARCHAR(2)))";
        } else {
            // DAILY default
            dateExpr = "SUBSTRING(CAST(d.disbursed_at AS VARCHAR(25)), 1, 10)";
        }

        String sql = "SELECT " + dateExpr + " AS period, la.branch_code, la.branch_name, " +
                "COUNT(d.disbursement_id) AS disb_count, COALESCE(SUM(d.amount), 0.00) AS total_amount " +
                "FROM DISBURSEMENT d " +
                "JOIN LOAN_APPLICATION la ON d.application_id = la.application_id " +
                "WHERE d.disbursed_at >= ? AND d.disbursed_at <= ? " +
                "GROUP BY " + dateExpr + ", la.branch_code, la.branch_name " +
                "ORDER BY period DESC, la.branch_code ASC";

        try {
            return jdbcTemplate.query(sql, (rs, rowNum) -> new DisbursementSummaryDTO(
                    rs.getString("period"),
                    rs.getString("branch_code"),
                    rs.getString("branch_name"),
                    rs.getLong("disb_count"),
                    rs.getBigDecimal("total_amount")
            ), startTs, endTs);
        } catch (DataAccessException e) {
            throw new DatabaseOperationException("Failed to generate Disbursement Summary: " + e.getMessage(), e);
        }
    }

    @Override
    public List<OverdueReviewReportDTO> getOverdueReviews(LocalDate businessDate) {
        LocalDate effectiveDate = (businessDate != null) ? businessDate : LocalDate.now();

        // Retrieve current UNDER_REVIEW applications
        String sql = "SELECT la.application_id, la.application_number, c.full_name AS customer_name, " +
                "c.mobile_number, la.product_type, la.requested_amount, la.submitted_at " +
                "FROM LOAN_APPLICATION la " +
                "JOIN CUSTOMER c ON la.customer_id = c.customer_id " +
                "WHERE la.status = 'UNDER_REVIEW' " +
                "ORDER BY la.submitted_at ASC";

        try {
            List<OverdueReviewReportDTO> allUnderReview = jdbcTemplate.query(sql, (rs, rowNum) -> {
                OverdueReviewReportDTO dto = new OverdueReviewReportDTO();
                dto.setApplicationId(rs.getLong("application_id"));
                dto.setApplicationNumber(rs.getString("application_number"));
                dto.setCustomerName(rs.getString("customer_name"));
                dto.setMobileNumber(rs.getString("mobile_number"));
                dto.setProductType(rs.getString("product_type"));
                dto.setRequestedAmount(rs.getBigDecimal("requested_amount"));
                Timestamp sub = rs.getTimestamp("submitted_at");
                dto.setSubmittedAt(sub != null ? sub.toLocalDateTime() : null);
                return dto;
            });

            // Filter for calendar days difference > 2
            List<OverdueReviewReportDTO> overdueList = new ArrayList<>();
            for (OverdueReviewReportDTO item : allUnderReview) {
                if (item.getSubmittedAt() != null) {
                    long days = java.time.temporal.ChronoUnit.DAYS.between(item.getSubmittedAt().toLocalDate(), effectiveDate);
                    if (days > 2) {
                        item.setDaysWaiting((int) days);
                        overdueList.add(item);
                    }
                }
            }
            return overdueList;
        } catch (DataAccessException e) {
            throw new DatabaseOperationException("Failed to generate Overdue Reviews Report: " + e.getMessage(), e);
        }
    }

    @Override
    public List<CustomerSummaryReportDTO> getCustomersWithoutDisbursement() {
        String sql = "SELECT DISTINCT c.customer_id, c.customer_code, c.full_name AS customer_name, " +
                "c.mobile_number, c.city, c.state, c.created_at AS customer_since " +
                "FROM CUSTOMER c " +
                "JOIN LOAN_APPLICATION la ON c.customer_id = la.customer_id " +
                "WHERE NOT EXISTS ( " +
                "    SELECT 1 FROM LOAN_APPLICATION la2 " +
                "    JOIN DISBURSEMENT d ON la2.application_id = d.application_id " +
                "    WHERE la2.customer_id = c.customer_id " +
                ") " +
                "ORDER BY c.customer_id ASC";
        try {
            return jdbcTemplate.query(sql, (rs, rowNum) -> {
                CustomerSummaryReportDTO dto = new CustomerSummaryReportDTO();
                dto.setCustomerId(rs.getLong("customer_id"));
                dto.setCustomerCode(rs.getString("customer_code"));
                dto.setCustomerName(rs.getString("customer_name"));
                dto.setMobileNumber(rs.getString("mobile_number"));
                dto.setCity(rs.getString("city"));
                dto.setState(rs.getString("state"));
                Timestamp ts = rs.getTimestamp("customer_since");
                dto.setCustomerSince(ts != null ? ts.toLocalDateTime() : null);
                return dto;
            });
        } catch (DataAccessException e) {
            throw new DatabaseOperationException("Failed to generate Customers Without Disbursement Report: " + e.getMessage(), e);
        }
    }

    @Override
    public List<RepeatApplicantReportDTO> getRepeatApplicants() {
        String sql = "SELECT c.customer_id, c.customer_code, c.full_name AS customer_name, c.mobile_number, " +
                "COUNT(la.application_id) AS total_apps, COALESCE(SUM(la.requested_amount), 0.00) AS total_amount, " +
                "MIN(la.created_at) AS first_date, MAX(la.created_at) AS latest_date " +
                "FROM CUSTOMER c " +
                "JOIN LOAN_APPLICATION la ON c.customer_id = la.customer_id " +
                "GROUP BY c.customer_id, c.customer_code, c.full_name, c.mobile_number " +
                "HAVING COUNT(la.application_id) > 1 " +
                "ORDER BY total_apps DESC, total_amount DESC";
        try {
            return jdbcTemplate.query(sql, (rs, rowNum) -> {
                RepeatApplicantReportDTO dto = new RepeatApplicantReportDTO();
                dto.setCustomerId(rs.getLong("customer_id"));
                dto.setCustomerCode(rs.getString("customer_code"));
                dto.setCustomerName(rs.getString("customer_name"));
                dto.setMobileNumber(rs.getString("mobile_number"));
                dto.setTotalApplications(rs.getLong("total_apps"));
                dto.setTotalRequestedAmount(rs.getBigDecimal("total_amount"));
                Timestamp fTs = rs.getTimestamp("first_date");
                dto.setFirstApplicationDate(fTs != null ? fTs.toLocalDateTime() : null);
                Timestamp lTs = rs.getTimestamp("latest_date");
                dto.setLatestApplicationDate(lTs != null ? lTs.toLocalDateTime() : null);
                return dto;
            });
        } catch (DataAccessException e) {
            throw new DatabaseOperationException("Failed to generate Repeat Applicants Report: " + e.getMessage(), e);
        }
    }
}
