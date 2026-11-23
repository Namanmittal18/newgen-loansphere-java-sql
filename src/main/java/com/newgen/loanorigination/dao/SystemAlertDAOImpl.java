package com.newgen.loanorigination.dao;

import com.newgen.loanorigination.exception.DatabaseOperationException;
import com.newgen.loanorigination.model.SystemAlert;
import com.newgen.loanorigination.model.enums.AlertType;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;

@Repository
public class SystemAlertDAOImpl implements SystemAlertDAO {

    private final JdbcTemplate jdbcTemplate;

    public SystemAlertDAOImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final RowMapper<SystemAlert> ALERT_ROW_MAPPER = (rs, rowNum) -> {
        SystemAlert alert = new SystemAlert();
        alert.setAlertId(rs.getLong("alert_id"));
        alert.setApplicationId(rs.getLong("application_id"));
        String type = rs.getString("alert_type");
        alert.setAlertType(type != null ? AlertType.valueOf(type) : null);
        Timestamp ts = rs.getTimestamp("created_at");
        alert.setCreatedAt(ts != null ? ts.toLocalDateTime() : null);

        // Optional joined fields if available
        try {
            alert.setApplicationNumber(rs.getString("application_number"));
            alert.setCustomerName(rs.getString("customer_name"));
        } catch (Exception ignored) {
        }

        return alert;
    };

    @Override
    public boolean saveIfAbsent(SystemAlert alert) {
        if (exists(alert.getApplicationId(), alert.getAlertType())) {
            return false;
        }

        String sql = "INSERT INTO SYSTEM_ALERTS (application_id, alert_type, created_at) VALUES (?, ?, ?)";
        try {
            jdbcTemplate.update(sql,
                    alert.getApplicationId(),
                    alert.getAlertType().name(),
                    alert.getCreatedAt() != null ? Timestamp.valueOf(alert.getCreatedAt()) : new Timestamp(System.currentTimeMillis()));
            return true;
        } catch (DuplicateKeyException e) {
            // Handled gracefully per idempotent requirement
            return false;
        } catch (DataAccessException e) {
            throw new DatabaseOperationException("Failed to save system alert: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean exists(Long applicationId, AlertType alertType) {
        String sql = "SELECT COUNT(*) FROM SYSTEM_ALERTS WHERE application_id = ? AND alert_type = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, applicationId, alertType.name());
        return count != null && count > 0;
    }

    @Override
    public List<SystemAlert> findByApplicationId(Long applicationId) {
        String sql = "SELECT a.*, la.application_number, c.full_name AS customer_name " +
                "FROM SYSTEM_ALERTS a " +
                "JOIN LOAN_APPLICATION la ON a.application_id = la.application_id " +
                "JOIN CUSTOMER c ON la.customer_id = c.customer_id " +
                "WHERE a.application_id = ? ORDER BY a.created_at DESC";
        try {
            return jdbcTemplate.query(sql, ALERT_ROW_MAPPER, applicationId);
        } catch (DataAccessException e) {
            throw new DatabaseOperationException("Failed to query alerts by application: " + e.getMessage(), e);
        }
    }

    @Override
    public List<SystemAlert> findAll() {
        String sql = "SELECT a.*, la.application_number, c.full_name AS customer_name " +
                "FROM SYSTEM_ALERTS a " +
                "JOIN LOAN_APPLICATION la ON a.application_id = la.application_id " +
                "JOIN CUSTOMER c ON la.customer_id = c.customer_id " +
                "ORDER BY a.created_at DESC";
        try {
            return jdbcTemplate.query(sql, ALERT_ROW_MAPPER);
        } catch (DataAccessException e) {
            throw new DatabaseOperationException("Failed to list alerts: " + e.getMessage(), e);
        }
    }

    @Override
    public long countTotal() {
        String sql = "SELECT COUNT(*) FROM SYSTEM_ALERTS";
        Long count = jdbcTemplate.queryForObject(sql, Long.class);
        return count != null ? count : 0L;
    }
}
