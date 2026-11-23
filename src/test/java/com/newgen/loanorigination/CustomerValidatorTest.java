package com.newgen.loanorigination;

import com.newgen.loanorigination.exception.ValidationException;
import com.newgen.loanorigination.validation.CustomerValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("2. Customer Validation & Age Boundary Tests")
class CustomerValidatorTest {

    private CustomerValidator validator;
    private final LocalDate businessDate = LocalDate.of(2026, 11, 23);

    @BeforeEach
    void setUp() {
        validator = new CustomerValidator();
    }

    @Test
    @DisplayName("Scenario 6: Age 20 completed years on submission is rejected")
    void testAge20Rejected() {
        // Born 2006-11-24: On 2026-11-23, exactly 19 years and 364 days -> 19 or 20 completed years
        LocalDate dobAge20 = businessDate.minusYears(20);
        ValidationException ex = assertThrows(ValidationException.class, () ->
                validator.validateDateOfBirth(dobAge20, businessDate));
        assertTrue(ex.getMessage().contains("must be between 21 and 60 completed years"));
    }

    @Test
    @DisplayName("Scenario 7: Age 21 completed years on submission is accepted")
    void testAge21Accepted() {
        LocalDate dobAge21 = businessDate.minusYears(21);
        assertDoesNotThrow(() -> validator.validateDateOfBirth(dobAge21, businessDate));
        assertEquals(21, validator.calculateAge(dobAge21, businessDate));
    }

    @Test
    @DisplayName("Scenario 8: Age 60 completed years on submission is accepted")
    void testAge60Accepted() {
        LocalDate dobAge60 = businessDate.minusYears(60);
        assertDoesNotThrow(() -> validator.validateDateOfBirth(dobAge60, businessDate));
        assertEquals(60, validator.calculateAge(dobAge60, businessDate));
    }

    @Test
    @DisplayName("Scenario 9: Age 61 completed years on submission is rejected")
    void testAge61Rejected() {
        LocalDate dobAge61 = businessDate.minusYears(61);
        ValidationException ex = assertThrows(ValidationException.class, () ->
                validator.validateDateOfBirth(dobAge61, businessDate));
        assertTrue(ex.getMessage().contains("must be between 21 and 60 completed years"));
    }

    @Test
    @DisplayName("Scenario 10: Future date of birth is rejected")
    void testFutureDobRejected() {
        LocalDate futureDob = businessDate.plusDays(1);
        ValidationException ex = assertThrows(ValidationException.class, () ->
                validator.validateDateOfBirth(futureDob, businessDate));
        assertTrue(ex.getMessage().contains("cannot be in the future"));
    }

    @Test
    @DisplayName("Mobile number validation: exactly 10 digits required")
    void testMobileNumberValidation() {
        assertDoesNotThrow(() -> validator.validateMobile("9876543210"));

        assertThrows(ValidationException.class, () -> validator.validateMobile("987654321")); // 9 digits
        assertThrows(ValidationException.class, () -> validator.validateMobile("98765432101")); // 11 digits
        assertThrows(ValidationException.class, () -> validator.validateMobile("98765ABCD0")); // non-numeric
        assertThrows(ValidationException.class, () -> validator.validateMobile("")); // empty
        assertThrows(ValidationException.class, () -> validator.validateMobile(null)); // null
    }
}
