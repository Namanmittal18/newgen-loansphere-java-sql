package com.newgen.loanorigination.dao;

import com.newgen.loanorigination.exception.DatabaseOperationException;
import com.newgen.loanorigination.model.Customer;
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
public class CustomerDAOImpl implements CustomerDAO {

    private final JdbcTemplate jdbcTemplate;

    public CustomerDAOImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final RowMapper<Customer> CUSTOMER_ROW_MAPPER = (rs, rowNum) -> {
        Customer c = new Customer();
        c.setCustomerId(rs.getLong("customer_id"));
        c.setCustomerCode(rs.getString("customer_code"));
        c.setFullName(rs.getString("full_name"));
        Date dob = rs.getDate("date_of_birth");
        c.setDateOfBirth(dob != null ? dob.toLocalDate() : null);
        c.setMobileNumber(rs.getString("mobile_number"));
        c.setEmail(rs.getString("email"));
        c.setAddressLine(rs.getString("address_line"));
        c.setCity(rs.getString("city"));
        c.setState(rs.getString("state"));
        c.setPostalCode(rs.getString("postal_code"));
        c.setKycType(rs.getString("kyc_type"));
        c.setKycReference(rs.getString("kyc_reference"));
        c.setKycStatus(rs.getString("kyc_status"));
        Timestamp cat = rs.getTimestamp("created_at");
        c.setCreatedAt(cat != null ? cat.toLocalDateTime() : null);
        Timestamp uat = rs.getTimestamp("updated_at");
        c.setUpdatedAt(uat != null ? uat.toLocalDateTime() : null);
        return c;
    };

    @Override
    public Customer save(Customer customer) {
        String sql = "INSERT INTO CUSTOMER (customer_code, full_name, date_of_birth, mobile_number, email, " +
                "address_line, city, state, postal_code, kyc_type, kyc_reference, kyc_status, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, customer.getCustomerCode());
                ps.setString(2, customer.getFullName());
                ps.setDate(3, customer.getDateOfBirth() != null ? Date.valueOf(customer.getDateOfBirth()) : null);
                ps.setString(4, customer.getMobileNumber());
                ps.setString(5, customer.getEmail());
                ps.setString(6, customer.getAddressLine());
                ps.setString(7, customer.getCity());
                ps.setString(8, customer.getState());
                ps.setString(9, customer.getPostalCode());
                ps.setString(10, customer.getKycType());
                ps.setString(11, customer.getKycReference());
                ps.setString(12, customer.getKycStatus() != null ? customer.getKycStatus() : "VERIFIED");
                ps.setTimestamp(13, customer.getCreatedAt() != null ?
                        Timestamp.valueOf(customer.getCreatedAt()) : new Timestamp(System.currentTimeMillis()));
                return ps;
            }, keyHolder);

            Long generatedId = com.newgen.loanorigination.utility.JdbcUtils.extractGeneratedKey(keyHolder, "customer_id");
            if (generatedId != null) {
                customer.setCustomerId(generatedId);
            }
            return customer;
        } catch (DataAccessException e) {
            throw new DatabaseOperationException("Failed to save customer record: " + e.getMessage(), e);
        }
    }

    @Override
    public void update(Customer customer) {
        String sql = "UPDATE CUSTOMER SET full_name = ?, email = ?, address_line = ?, city = ?, state = ?, " +
                "postal_code = ?, kyc_type = ?, kyc_reference = ?, kyc_status = ? WHERE customer_id = ?";
        try {
            jdbcTemplate.update(sql,
                    customer.getFullName(),
                    customer.getEmail(),
                    customer.getAddressLine(),
                    customer.getCity(),
                    customer.getState(),
                    customer.getPostalCode(),
                    customer.getKycType(),
                    customer.getKycReference(),
                    customer.getKycStatus(),
                    customer.getCustomerId());
        } catch (DataAccessException e) {
            throw new DatabaseOperationException("Failed to update customer record: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Customer> findById(Long customerId) {
        String sql = "SELECT * FROM CUSTOMER WHERE customer_id = ?";
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, CUSTOMER_ROW_MAPPER, customerId));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        } catch (DataAccessException e) {
            throw new DatabaseOperationException("Failed to query customer by ID: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Customer> findByCustomerCode(String customerCode) {
        String sql = "SELECT * FROM CUSTOMER WHERE customer_code = ?";
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, CUSTOMER_ROW_MAPPER, customerCode));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        } catch (DataAccessException e) {
            throw new DatabaseOperationException("Failed to query customer by code: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Customer> findByMobileNumber(String mobileNumber) {
        String sql = "SELECT * FROM CUSTOMER WHERE mobile_number = ?";
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, CUSTOMER_ROW_MAPPER, mobileNumber));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        } catch (DataAccessException e) {
            throw new DatabaseOperationException("Failed to query customer by mobile: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Customer> searchByName(String nameQuery) {
        String sql = "SELECT * FROM CUSTOMER WHERE LOWER(full_name) LIKE LOWER(?) ORDER BY customer_id DESC";
        try {
            return jdbcTemplate.query(sql, CUSTOMER_ROW_MAPPER, "%" + nameQuery.trim() + "%");
        } catch (DataAccessException e) {
            throw new DatabaseOperationException("Failed to search customers: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Customer> findAll() {
        String sql = "SELECT * FROM CUSTOMER ORDER BY customer_id DESC";
        try {
            return jdbcTemplate.query(sql, CUSTOMER_ROW_MAPPER);
        } catch (DataAccessException e) {
            throw new DatabaseOperationException("Failed to list all customers: " + e.getMessage(), e);
        }
    }

    @Override
    public long countTotal() {
        String sql = "SELECT COUNT(*) FROM CUSTOMER";
        Long count = jdbcTemplate.queryForObject(sql, Long.class);
        return count != null ? count : 0L;
    }

    @Override
    public boolean existsByMobileNumber(String mobileNumber) {
        String sql = "SELECT COUNT(*) FROM CUSTOMER WHERE mobile_number = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, mobileNumber);
        return count != null && count > 0;
    }
}
