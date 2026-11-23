package com.newgen.loanorigination.service;

import com.newgen.loanorigination.config.BusinessClock;
import com.newgen.loanorigination.dao.CustomerDAO;
import com.newgen.loanorigination.exception.CustomerNotFoundException;
import com.newgen.loanorigination.exception.ValidationException;
import com.newgen.loanorigination.model.Customer;
import com.newgen.loanorigination.utility.ReferenceGenerator;
import com.newgen.loanorigination.validation.CustomerValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class CustomerService {

    private final CustomerDAO customerDAO;
    private final CustomerValidator customerValidator;
    private final BusinessClock businessClock;

    public CustomerService(CustomerDAO customerDAO, CustomerValidator customerValidator, BusinessClock businessClock) {
        this.customerDAO = customerDAO;
        this.customerValidator = customerValidator;
        this.businessClock = businessClock;
    }

    @Transactional
    public Customer registerCustomer(Customer customer) {
        LocalDate businessDate = businessClock.getCurrentDate();
        customerValidator.validate(customer, businessDate);

        if (customerDAO.existsByMobileNumber(customer.getMobileNumber().trim())) {
            throw new ValidationException(String.format("Mobile number '%s' is already registered with another customer.",
                    customer.getMobileNumber().trim()));
        }

        if (customer.getCustomerCode() == null || customer.getCustomerCode().isBlank()) {
            customer.setCustomerCode(ReferenceGenerator.generateCustomerCode(businessDate));
        }

        customer.setCreatedAt(businessClock.getCurrentDateTime());
        return customerDAO.save(customer);
    }

    @Transactional
    public void updateCustomer(Customer customer) {
        if (customer.getCustomerId() == null) {
            throw new ValidationException("Customer ID is required for updating.");
        }
        getCustomerById(customer.getCustomerId()); // verify exists
        customerDAO.update(customer);
    }

    public Customer getCustomerById(Long customerId) {
        return customerDAO.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found with ID: " + customerId));
    }

    public Customer getCustomerByCode(String customerCode) {
        return customerDAO.findByCustomerCode(customerCode)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found with code: " + customerCode));
    }

    public Customer getCustomerByMobile(String mobileNumber) {
        return customerDAO.findByMobileNumber(mobileNumber)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found with mobile: " + mobileNumber));
    }

    public List<Customer> searchCustomers(String query) {
        if (query == null || query.trim().isEmpty()) {
            return customerDAO.findAll();
        }
        String cleanQuery = query.trim();
        if (cleanQuery.matches("^[0-9]+$") && cleanQuery.length() == 10) {
            return customerDAO.findByMobileNumber(cleanQuery).map(List::of).orElse(List.of());
        }
        if (cleanQuery.toUpperCase().startsWith("CUST-")) {
            return customerDAO.findByCustomerCode(cleanQuery).map(List::of).orElse(List.of());
        }
        return customerDAO.searchByName(cleanQuery);
    }

    public List<Customer> getAllCustomers() {
        return customerDAO.findAll();
    }

    public long getTotalCustomersCount() {
        return customerDAO.countTotal();
    }
}
