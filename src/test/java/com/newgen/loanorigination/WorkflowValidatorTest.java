package com.newgen.loanorigination;

import com.newgen.loanorigination.exception.InvalidApplicationStateException;
import com.newgen.loanorigination.exception.ValidationException;
import com.newgen.loanorigination.model.enums.ApplicationStatus;
import com.newgen.loanorigination.validation.WorkflowValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("4. Workflow State Control & Transition Tests")
class WorkflowValidatorTest {

    private WorkflowValidator validator;

    @BeforeEach
    void setUp() {
        validator = new WorkflowValidator();
    }

    @Test
    @DisplayName("Valid canonical lifecycle transitions are allowed")
    void testValidTransitions() {
        assertDoesNotThrow(() -> validator.validateTransition(ApplicationStatus.DRAFT, ApplicationStatus.UNDER_REVIEW));
        assertDoesNotThrow(() -> validator.validateTransition(ApplicationStatus.DRAFT, ApplicationStatus.REJECTED));
        assertDoesNotThrow(() -> validator.validateTransition(ApplicationStatus.UNDER_REVIEW, ApplicationStatus.CREDIT_APPROVAL));
        assertDoesNotThrow(() -> validator.validateTransition(ApplicationStatus.UNDER_REVIEW, ApplicationStatus.REJECTED));
        assertDoesNotThrow(() -> validator.validateTransition(ApplicationStatus.CREDIT_APPROVAL, ApplicationStatus.DISBURSED));
        assertDoesNotThrow(() -> validator.validateTransition(ApplicationStatus.CREDIT_APPROVAL, ApplicationStatus.REJECTED));
        assertDoesNotThrow(() -> validator.validateTransition(ApplicationStatus.DISBURSED, ApplicationStatus.CORE_BANKING_PUSH));
        assertDoesNotThrow(() -> validator.validateTransition(ApplicationStatus.CORE_BANKING_PUSH, ApplicationStatus.EXIT));
    }

    @Test
    @DisplayName("Scenario 19: Invalid state transitions are strictly rejected")
    void testInvalidTransitionsRejected() {
        // DRAFT directly to DISBURSED
        assertThrows(InvalidApplicationStateException.class, () ->
                validator.validateTransition(ApplicationStatus.DRAFT, ApplicationStatus.DISBURSED));

        // UNDER_REVIEW directly to DISBURSED
        assertThrows(InvalidApplicationStateException.class, () ->
                validator.validateTransition(ApplicationStatus.UNDER_REVIEW, ApplicationStatus.DISBURSED));

        // DISBURSED back to DRAFT
        assertThrows(InvalidApplicationStateException.class, () ->
                validator.validateTransition(ApplicationStatus.DISBURSED, ApplicationStatus.DRAFT));

        // REJECTED to any other state (terminal)
        assertThrows(InvalidApplicationStateException.class, () ->
                validator.validateTransition(ApplicationStatus.REJECTED, ApplicationStatus.UNDER_REVIEW));

        // EXIT to any other state (terminal)
        assertThrows(InvalidApplicationStateException.class, () ->
                validator.validateTransition(ApplicationStatus.EXIT, ApplicationStatus.DRAFT));
    }

    @Test
    @DisplayName("Scenario 20: Missing rejection reason is rejected")
    void testMissingRejectionReasonRejected() {
        assertThrows(ValidationException.class, () ->
                validator.validateRejectionReason(ApplicationStatus.REJECTED, null));

        assertThrows(ValidationException.class, () ->
                validator.validateRejectionReason(ApplicationStatus.REJECTED, "   "));

        assertDoesNotThrow(() ->
                validator.validateRejectionReason(ApplicationStatus.REJECTED, "Applicant failed credit risk criteria."));
    }
}
