package com.newgen.loanorigination.dao;

import com.newgen.loanorigination.exception.DatabaseOperationException;
import com.newgen.loanorigination.model.Disbursement;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Repository
public class DisbursementDAOImpl implements DisbursementDAO {

    private final JdbcTemplate jdbcTemplate;

    public DisbursementDAOImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final RowMapper<Disbursement> DISBURSEMENT_ROW_MAPPER = (rs, rowNum) -> {
        Disbursement d = new Disbursement();
        d.setDisbursementId(rs.getLong("disbursement_id"));
        d.setApplicationId(rs.getLong("application_id"));
        d.setDisbursementReference(rs.getString("disbursement_reference"));
        d.setAmount(rs.getBigDecimal("amount"));
        Timestamp ts = rs.getTimestamp("disbursed_at");
        d.setDisbursedAt(ts != null ? ts.toLocalDateTime() : null);
        return d;
    };

    @Override
    public Disbursement save(Disbursement disbursement) {
        String sql = "INSERT INTO DISBURSEMENT (application_id, disbursement_reference, amount, disbursed_at) " +
                "VALUES (?, ?, ?, ?)";
        try {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                ps.setLong(1, disbursement.getApplicationId());
                ps.setString(2, disbursement.getDisbursementReference());
                ps.setBigDecimal(3, disbursement.getAmount());
                ps.setTimestamp(4, disbursement.getDisbursedAt() != null ?
                        Timestamp.valueOf(disbursement.getDisbursedAt()) : new Timestamp(System.currentTimeMillis()));
                return ps;
            }, keyHolder);

            Long generatedId = com.newgen.loanorigination.utility.JdbcUtils.extractGeneratedKey(keyHolder, "disbursement_id");
            if (generatedId != null) {
                disbursement.setDisbursementId(generatedId);
            }
            return disbursement;
        } catch (DataAccessException e) {
            throw new DatabaseOperationException("Failed to save disbursement record: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Disbursement> findByApplicationId(Long applicationId) {
        String sql = "SELECT * FROM DISBURSEMENT WHERE application_id = ?";
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, DISBURSEMENT_ROW_MAPPER, applicationId));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        } catch (DataAccessException e) {
            throw new DatabaseOperationException("Failed to query disbursement by application: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Disbursement> findByReference(String reference) {
        String sql = "SELECT * FROM DISBURSEMENT WHERE disbursement_reference = ?";
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, DISBURSEMENT_ROW_MAPPER, reference));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        } catch (DataAccessException e) {
            throw new DatabaseOperationException("Failed to query disbursement by reference: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean existsByApplicationId(Long applicationId) {
        String sql = "SELECT COUNT(*) FROM DISBURSEMENT WHERE application_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, applicationId);
        return count != null && count > 0;
    }

    @Override
    public boolean existsByReference(String reference) {
        String sql = "SELECT COUNT(*) FROM DISBURSEMENT WHERE disbursement_reference = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, reference);
        return count != null && count > 0;
    }

    @Override
    public BigDecimal sumTotalDisbursedAmount() {
        String sql = "SELECT COALESCE(SUM(amount), 0.00) FROM DISBURSEMENT";
        BigDecimal sum = jdbcTemplate.queryForObject(sql, BigDecimal.class);
        return sum != null ? sum : BigDecimal.ZERO;
    }

    @Override
    public long countTotal() {
        String sql = "SELECT COUNT(*) FROM DISBURSEMENT";
        Long count = jdbcTemplate.queryForObject(sql, Long.class);
        return count != null ? count : 0L;
    }

    @Override
    public List<Disbursement> findAll() {
        String sql = "SELECT * FROM DISBURSEMENT ORDER BY disbursed_at DESC";
        try {
            return jdbcTemplate.query(sql, DISBURSEMENT_ROW_MAPPER);
        } catch (DataAccessException e) {
            throw new DatabaseOperationException("Failed to list all disbursements: " + e.getMessage(), e);
        }
    }
}
