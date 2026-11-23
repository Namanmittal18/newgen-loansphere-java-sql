package com.newgen.loanorigination.validation;

import com.newgen.loanorigination.exception.ValidationException;
import com.newgen.loanorigination.model.Customer;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Period;

@Component
public class CustomerValidator {

    public static final int MIN_AGE = 21;
    public static final int MAX_AGE = 60;

    public void validate(Customer customer, LocalDate businessDate) {
        if (customer == null) {
            throw new ValidationException("Customer information is required.");
        }

        if (customer.getFullName() == null || customer.getFullName().trim().isEmpty()) {
            throw new ValidationException("Customer full name is required.");
        }

        validateMobile(customer.getMobileNumber());
        validateDateOfBirth(customer.getDateOfBirth(), businessDate);
    }

    public void validateMobile(String mobileNumber) {
        if (mobileNumber == null || !mobileNumber.trim().matches("^[0-9]{10}$")) {
            throw new ValidationException("Mobile number must contain exactly 10 digits.");
        }
    }

    public void validateDateOfBirth(LocalDate dateOfBirth, LocalDate businessDate) {
        if (dateOfBirth == null) {
            throw new ValidationException("Date of birth is required.");
        }

        LocalDate effectiveDate = (businessDate != null) ? businessDate : LocalDate.now();

        if (dateOfBirth.isAfter(effectiveDate)) {
            throw new ValidationException("Date of birth cannot be in the future.");
        }

        int completedAge = calculateAge(dateOfBirth, effectiveDate);
        if (completedAge < MIN_AGE || completedAge > MAX_AGE) {
            throw new ValidationException(String.format(
                    "Customer must be between %d and %d completed years old on submission. Calculated age: %d years.",
                    MIN_AGE, MAX_AGE, completedAge));
        }
    }

    public int calculateAge(LocalDate dateOfBirth, LocalDate effectiveDate) {
        if (dateOfBirth == null || effectiveDate == null) {
            return 0;
        }
        return Period.between(dateOfBirth, effectiveDate).getYears();
    }
}
