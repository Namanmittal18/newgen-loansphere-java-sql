package com.newgen.loanorigination.dao;

import com.newgen.loanorigination.exception.DatabaseOperationException;
import com.newgen.loanorigination.model.ApplicationHistory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;

@Repository
public class ApplicationHistoryDAOImpl implements ApplicationHistoryDAO {

    private final JdbcTemplate jdbcTemplate;

    public ApplicationHistoryDAOImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final RowMapper<ApplicationHistory> HISTORY_ROW_MAPPER = (rs, rowNum) -> {
        ApplicationHistory h = new ApplicationHistory();
        h.setHistoryId(rs.getLong("history_id"));
        h.setApplicationId(rs.getLong("application_id"));
        h.setAction(rs.getString("action"));
        h.setOldStatus(rs.getString("old_status"));
        h.setNewStatus(rs.getString("new_status"));
        h.setPerformedBy(rs.getString("performed_by"));
        Timestamp ts = rs.getTimestamp("performed_at");
        h.setPerformedAt(ts != null ? ts.toLocalDateTime() : null);
        h.setRemarks(rs.getString("remarks"));
        return h;
    };

    @Override
    public void save(ApplicationHistory history) {
        String sql = "INSERT INTO APPLICATION_HISTORY (application_id, action, old_status, new_status, " +
                "performed_by, performed_at, remarks) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try {
            jdbcTemplate.update(sql,
                    history.getApplicationId(),
                    history.getAction(),
                    history.getOldStatus(),
                    history.getNewStatus(),
                    history.getPerformedBy(),
                    history.getPerformedAt() != null ? Timestamp.valueOf(history.getPerformedAt()) : new Timestamp(System.currentTimeMillis()),
                    history.getRemarks());
        } catch (DataAccessException e) {
            throw new DatabaseOperationException("Failed to save application history: " + e.getMessage(), e);
        }
    }

    @Override
    public List<ApplicationHistory> findByApplicationId(Long applicationId) {
        String sql = "SELECT * FROM APPLICATION_HISTORY WHERE application_id = ? ORDER BY performed_at ASC, history_id ASC";
        try {
            return jdbcTemplate.query(sql, HISTORY_ROW_MAPPER, applicationId);
        } catch (DataAccessException e) {
            throw new DatabaseOperationException("Failed to query application history: " + e.getMessage(), e);
        }
    }

    @Override
    public List<ApplicationHistory> findRecent(int limit) {
        String sql = "SELECT * FROM APPLICATION_HISTORY ORDER BY performed_at DESC LIMIT ?";
        try {
            return jdbcTemplate.query(sql, HISTORY_ROW_MAPPER, limit);
        } catch (DataAccessException e) {
            throw new DatabaseOperationException("Failed to query recent application history: " + e.getMessage(), e);
        }
    }
}
