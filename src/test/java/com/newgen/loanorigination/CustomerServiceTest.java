package com.newgen.loanorigination;

import com.newgen.loanorigination.config.BusinessClock;
import com.newgen.loanorigination.dao.CustomerDAO;
import com.newgen.loanorigination.exception.ValidationException;
import com.newgen.loanorigination.model.Customer;
import com.newgen.loanorigination.service.CustomerService;
import com.newgen.loanorigination.validation.CustomerValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("9. Customer Service & Registration Integrity Tests")
class CustomerServiceTest {

    private CustomerDAO customerDAO;
    private CustomerService customerService;
    private BusinessClock businessClock;

    @BeforeEach
    void setUp() {
        customerDAO = mock(CustomerDAO.class);
        CustomerValidator customerValidator = new CustomerValidator();
        businessClock = new BusinessClock("2026-11-23");
        customerService = new CustomerService(customerDAO, customerValidator, businessClock);
    }

    private Customer createValidCustomer(String mobileNumber, String fullName) {
        Customer customer = new Customer();
        customer.setFullName(fullName);
        customer.setMobileNumber(mobileNumber);
        // Completed age on 2026-11-23: 31 years (within [21, 60])
        customer.setDateOfBirth(LocalDate.of(1995, 5, 15));
        customer.setEmail("applicant@example.com");
        customer.setAddressLine("Flat 101, Green Meadows");
        customer.setCity("Noida");
        customer.setState("Uttar Pradesh");
        customer.setPostalCode("201301");
        customer.setKycType("AADHAAR");
        customer.setKycReference("1234-5678-9012");
        return customer;
    }

    @Test
    @DisplayName("Scenario 11: Duplicate mobile number registration is rejected with ValidationException")
    void testDuplicateMobileRegistrationRejected() {
        String existingMobile = "9876543210";

        // Setup mock: mobile already exists in the database
        when(customerDAO.existsByMobileNumber(existingMobile)).thenReturn(true);

        Customer newCustomer = createValidCustomer(existingMobile, "Second Applicant");

        ValidationException ex = assertThrows(ValidationException.class, () ->
                customerService.registerCustomer(newCustomer));

        assertTrue(ex.getMessage().contains("already registered"),
                "Exception message should state that the mobile number is already registered. Actual: " + ex.getMessage());
        assertTrue(ex.getMessage().contains(existingMobile));

        // Verify that save() was NEVER invoked
        verify(customerDAO, never()).save(any(Customer.class));
    }

    @Test
    @DisplayName("Verification: Existing customer records remain unchanged when duplicate registration is attempted")
    void testExistingCustomerUnchangedOnDuplicateAttempt() {
        String existingMobile = "9876543210";

        Customer existingCustomer = createValidCustomer(existingMobile, "Original Customer");
        existingCustomer.setCustomerId(101L);
        existingCustomer.setCustomerCode("CUST-20260101-0001");

        when(customerDAO.existsByMobileNumber(existingMobile)).thenReturn(true);
        when(customerDAO.findByMobileNumber(existingMobile)).thenReturn(Optional.of(existingCustomer));

        // Another registration attempt with identical mobile but conflicting name and details
        Customer duplicateAttempt = createValidCustomer(existingMobile, "Intruder Attempt");

        assertThrows(ValidationException.class, () ->
                customerService.registerCustomer(duplicateAttempt));

        // Verify that the existing customer record is never mutated, saved, or updated
        verify(customerDAO, never()).save(any(Customer.class));
        verify(customerDAO, never()).update(any(Customer.class));

        // Verify existing customer in DAO still retains original details
        Optional<Customer> retrieved = customerDAO.findByMobileNumber(existingMobile);
        assertTrue(retrieved.isPresent());
        assertEquals(101L, retrieved.get().getCustomerId());
        assertEquals("Original Customer", retrieved.get().getFullName());
        assertEquals("CUST-20260101-0001", retrieved.get().getCustomerCode());
    }

    @Test
    @DisplayName("Successful registration: Customer with unique mobile is saved with generated code")
    void testSuccessfulCustomerRegistration() {
        String uniqueMobile = "9876543219";

        when(customerDAO.existsByMobileNumber(uniqueMobile)).thenReturn(false);
        when(customerDAO.save(any(Customer.class))).thenAnswer(invocation -> {
            Customer saved = invocation.getArgument(0);
            saved.setCustomerId(1L);
            return saved;
        });

        Customer customer = createValidCustomer(uniqueMobile, "Aarav Sharma");
        Customer registered = customerService.registerCustomer(customer);

        assertNotNull(registered);
        assertEquals(1L, registered.getCustomerId());
        assertNotNull(registered.getCustomerCode());
        assertTrue(registered.getCustomerCode().startsWith("CUST-"));
        assertEquals(uniqueMobile, registered.getMobileNumber());
        assertNotNull(registered.getCreatedAt());

        verify(customerDAO, times(1)).existsByMobileNumber(uniqueMobile);
        verify(customerDAO, times(1)).save(customer);
    }
}
