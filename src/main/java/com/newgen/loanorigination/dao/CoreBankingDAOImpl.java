package com.newgen.loanorigination.dao;

import com.newgen.loanorigination.exception.DatabaseOperationException;
import com.newgen.loanorigination.model.CoreBankingRecord;
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
import java.util.List;
import java.util.Optional;

@Repository
public class CoreBankingDAOImpl implements CoreBankingDAO {

    private final JdbcTemplate jdbcTemplate;

    public CoreBankingDAOImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final RowMapper<CoreBankingRecord> CORE_BANKING_ROW_MAPPER = (rs, rowNum) -> {
        CoreBankingRecord rec = new CoreBankingRecord();
        rec.setCoreBankingId(rs.getLong("core_banking_id"));
        rec.setCustomerId(rs.getLong("customer_id"));
        rec.setApplicationId(rs.getLong("application_id"));
        rec.setCustomerName(rs.getString("customer_name"));
        rec.setAddress(rs.getString("address"));
        Date dob = rs.getDate("date_of_birth");
        rec.setDateOfBirth(dob != null ? dob.toLocalDate() : null);
        rec.setKycDetails(rs.getString("kyc_details"));
        rec.setApprovedAmount(rs.getBigDecimal("approved_amount"));
        rec.setDisbursedAmount(rs.getBigDecimal("disbursed_amount"));
        Timestamp disbDate = rs.getTimestamp("disbursed_date");
        rec.setDisbursedDate(disbDate != null ? disbDate.toLocalDateTime() : null);
        Timestamp pushed = rs.getTimestamp("pushed_at");
        rec.setPushedAt(pushed != null ? pushed.toLocalDateTime() : null);
        return rec;
    };

    @Override
    public CoreBankingRecord save(CoreBankingRecord record) {
        String sql = "INSERT INTO NG_CORE_BANKING (customer_id, application_id, customer_name, address, " +
                "date_of_birth, kyc_details, approved_amount, disbursed_amount, disbursed_date, pushed_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                ps.setLong(1, record.getCustomerId());
                ps.setLong(2, record.getApplicationId());
                ps.setString(3, record.getCustomerName());
                ps.setString(4, record.getAddress());
                ps.setDate(5, record.getDateOfBirth() != null ? Date.valueOf(record.getDateOfBirth()) : null);
                ps.setString(6, record.getKycDetails());
                ps.setBigDecimal(7, record.getApprovedAmount());
                ps.setBigDecimal(8, record.getDisbursedAmount());
                ps.setTimestamp(9, record.getDisbursedDate() != null ?
                        Timestamp.valueOf(record.getDisbursedDate()) : new Timestamp(System.currentTimeMillis()));
                ps.setTimestamp(10, record.getPushedAt() != null ?
                        Timestamp.valueOf(record.getPushedAt()) : new Timestamp(System.currentTimeMillis()));
                return ps;
            }, keyHolder);

            if (keyHolder.getKey() != null) {
                record.setCoreBankingId(keyHolder.getKey().longValue());
            }
            return record;
        } catch (DataAccessException e) {
            throw new DatabaseOperationException("Failed to save core banking record: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<CoreBankingRecord> findByApplicationId(Long applicationId) {
        String sql = "SELECT * FROM NG_CORE_BANKING WHERE application_id = ?";
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, CORE_BANKING_ROW_MAPPER, applicationId));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        } catch (DataAccessException e) {
            throw new DatabaseOperationException("Failed to query core banking record: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean existsByApplicationId(Long applicationId) {
        String sql = "SELECT COUNT(*) FROM NG_CORE_BANKING WHERE application_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, applicationId);
        return count != null && count > 0;
    }

    @Override
    public List<CoreBankingRecord> findAll() {
        String sql = "SELECT * FROM NG_CORE_BANKING ORDER BY pushed_at DESC";
        try {
            return jdbcTemplate.query(sql, CORE_BANKING_ROW_MAPPER);
        } catch (DataAccessException e) {
            throw new DatabaseOperationException("Failed to list core banking records: " + e.getMessage(), e);
        }
    }

    @Override
    public long countTotal() {
        String sql = "SELECT COUNT(*) FROM NG_CORE_BANKING";
        Long count = jdbcTemplate.queryForObject(sql, Long.class);
        return count != null ? count : 0L;
    }
}
