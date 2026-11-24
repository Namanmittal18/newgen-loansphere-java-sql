package com.newgen.loanorigination.dao;

import com.newgen.loanorigination.exception.DatabaseOperationException;
import com.newgen.loanorigination.model.UtilityRun;
import com.newgen.loanorigination.model.enums.UtilityResult;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class UtilityRunDAOImpl implements UtilityRunDAO {

    private final JdbcTemplate jdbcTemplate;

    public UtilityRunDAOImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final RowMapper<UtilityRun> UTILITY_RUN_ROW_MAPPER = (rs, rowNum) -> {
        UtilityRun run = new UtilityRun();
        run.setRunId(rs.getLong("run_id"));
        run.setUtilityName(rs.getString("utility_name"));
        Timestamp start = rs.getTimestamp("started_at");
        run.setStartedAt(start != null ? start.toLocalDateTime() : null);
        Timestamp comp = rs.getTimestamp("completed_at");
        run.setCompletedAt(comp != null ? comp.toLocalDateTime() : null);
        String res = rs.getString("result");
        run.setResult(res != null ? UtilityResult.valueOf(res) : null);
        run.setApplicationsProcessed(rs.getInt("applications_processed"));
        run.setAlertsCreated(rs.getInt("alerts_created"));
        run.setErrorMessage(rs.getString("error_message"));
        return run;
    };

    @Override
    public Long startRun(String utilityName, LocalDateTime startedAt) {
        String sql = "INSERT INTO UTILITY_RUN (utility_name, started_at, result) VALUES (?, ?, ?)";
        try {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, utilityName);
                ps.setTimestamp(2, startedAt != null ? Timestamp.valueOf(startedAt) : new Timestamp(System.currentTimeMillis()));
                ps.setString(3, UtilityResult.IN_PROGRESS.name());
                return ps;
            }, keyHolder);

            Long generatedId = com.newgen.loanorigination.utility.JdbcUtils.extractGeneratedKey(keyHolder, "run_id");
            if (generatedId != null) {
                return generatedId;
            }
            throw new DatabaseOperationException("Failed to obtain generated ID for utility run.");
        } catch (DataAccessException e) {
            throw new DatabaseOperationException("Failed to record utility run start: " + e.getMessage(), e);
        }
    }

    @Override
    public void completeRun(Long runId, UtilityResult result, int applicationsProcessed, int alertsCreated,
                            String errorMessage, LocalDateTime completedAt) {
        String sql = "UPDATE UTILITY_RUN SET completed_at = ?, result = ?, applications_processed = ?, " +
                "alerts_created = ?, error_message = ? WHERE run_id = ?";
        try {
            jdbcTemplate.update(sql,
                    completedAt != null ? Timestamp.valueOf(completedAt) : new Timestamp(System.currentTimeMillis()),
                    result.name(),
                    applicationsProcessed,
                    alertsCreated,
                    errorMessage,
                    runId);
        } catch (DataAccessException e) {
            throw new DatabaseOperationException("Failed to complete utility run: " + e.getMessage(), e);
        }
    }

    @Override
    public List<UtilityRun> findAll() {
        String sql = "SELECT * FROM UTILITY_RUN ORDER BY started_at DESC LIMIT 100";
        try {
            return jdbcTemplate.query(sql, UTILITY_RUN_ROW_MAPPER);
        } catch (DataAccessException e) {
            throw new DatabaseOperationException("Failed to list utility runs: " + e.getMessage(), e);
        }
    }
}
