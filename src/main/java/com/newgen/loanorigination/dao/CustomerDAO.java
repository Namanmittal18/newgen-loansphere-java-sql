package com.newgen.loanorigination.dao;

import com.newgen.loanorigination.model.Customer;

import java.util.List;
import java.util.Optional;

public interface CustomerDAO {
    Customer save(Customer customer);
    void update(Customer customer);
    Optional<Customer> findById(Long customerId);
    Optional<Customer> findByCustomerCode(String customerCode);
    Optional<Customer> findByMobileNumber(String mobileNumber);
    List<Customer> searchByName(String nameQuery);
    List<Customer> findAll();
    long countTotal();
    boolean existsByMobileNumber(String mobileNumber);
}
