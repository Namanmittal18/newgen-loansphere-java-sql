package com.newgen.loanorigination;

import com.newgen.loanorigination.config.BusinessClock;
import com.newgen.loanorigination.dao.ApplicationHistoryDAO;
import com.newgen.loanorigination.dao.CustomerDAO;
import com.newgen.loanorigination.dao.DisbursementDAO;
import com.newgen.loanorigination.dao.LoanApplicationDAO;
import com.newgen.loanorigination.dto.DisbursementResponse;
import com.newgen.loanorigination.exception.DisbursementException;
import com.newgen.loanorigination.exception.InvalidApplicationStateException;
import com.newgen.loanorigination.model.Customer;
import com.newgen.loanorigination.model.Disbursement;
import com.newgen.loanorigination.model.LoanApplication;
import com.newgen.loanorigination.model.enums.ApplicationStatus;
import com.newgen.loanorigination.model.enums.ProductType;
import com.newgen.loanorigination.service.DisbursementService;
import com.newgen.loanorigination.validation.WorkflowValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("6. Disbursement Integrity & Validation Tests")
class DisbursementServiceTest {

    private LoanApplicationDAO loanApplicationDAO;
    private CustomerDAO customerDAO;
    private DisbursementDAO disbursementDAO;
    private ApplicationHistoryDAO historyDAO;
    private DisbursementService disbursementService;

    @BeforeEach
    void setUp() {
        loanApplicationDAO = mock(LoanApplicationDAO.class);
        customerDAO = mock(CustomerDAO.class);
        disbursementDAO = mock(DisbursementDAO.class);
        historyDAO = mock(ApplicationHistoryDAO.class);

        BusinessClock clock = new BusinessClock("2026-11-23");
        WorkflowValidator workflowValidator = new WorkflowValidator();

        disbursementService = new DisbursementService(
                loanApplicationDAO,
                customerDAO,
                disbursementDAO,
                historyDAO,
                workflowValidator,
                clock
        );
    }

    private LoanApplication createMockApp(Long id, ApplicationStatus status) {
        LoanApplication app = new LoanApplication();
        app.setApplicationId(id);
        app.setApplicationNumber("APP-20261123-0001");
        app.setCustomerId(1L);
        app.setProductType(ProductType.PERSONAL);
        app.setRequestedAmount(new BigDecimal("100000.00"));
        app.setApprovedAmount(new BigDecimal("100000.00"));
        app.setStatus(status);
        return app;
    }

    @Test
    @DisplayName("Scenario 23: Disbursement from any status other than CREDIT_APPROVAL is rejected")
    void testDisbursementBeforeApprovalRejected() {
        // Application still in UNDER_REVIEW
        LoanApplication underReviewApp = createMockApp(1L, ApplicationStatus.UNDER_REVIEW);
        when(loanApplicationDAO.findById(1L)).thenReturn(Optional.of(underReviewApp));

        assertThrows(InvalidApplicationStateException.class, () ->
                disbursementService.disburseLoan(1L, "TEST_USER"));

        // Application in DRAFT
        LoanApplication draftApp = createMockApp(2L, ApplicationStatus.DRAFT);
        when(loanApplicationDAO.findById(2L)).thenReturn(Optional.of(draftApp));

        assertThrows(InvalidApplicationStateException.class, () ->
                disbursementService.disburseLoan(2L, "TEST_USER"));
    }

    @Test
    @DisplayName("Scenario 22: Repeated disbursement on already disbursed application is rejected")
    void testRepeatedDisbursementRejected() {
        LoanApplication approvedApp = createMockApp(1L, ApplicationStatus.CREDIT_APPROVAL);
        when(loanApplicationDAO.findById(1L)).thenReturn(Optional.of(approvedApp));
        // Already exists in disbursement table
        when(disbursementDAO.existsByApplicationId(1L)).thenReturn(true);

        assertThrows(DisbursementException.class, () ->
                disbursementService.disburseLoan(1L, "TEST_USER"));
    }

    @Test
    @DisplayName("Successful disbursement execution advances to DISBURSED and returns confirmation")
    void testSuccessfulDisbursement() {
        LoanApplication app = createMockApp(1L, ApplicationStatus.CREDIT_APPROVAL);
        when(loanApplicationDAO.findById(1L)).thenReturn(Optional.of(app));
        when(disbursementDAO.existsByApplicationId(1L)).thenReturn(false);
        when(disbursementDAO.existsByReference(any())).thenReturn(false);

        Customer customer = new Customer();
        customer.setCustomerId(1L);
        customer.setFullName("Aarav Sharma");
        when(customerDAO.findById(1L)).thenReturn(Optional.of(customer));

        DisbursementResponse response = disbursementService.disburseLoan(1L, "OPS_USER");

        assertNotNull(response);
        assertEquals("APP-20261123-0001", response.getApplicationNumber());
        assertEquals("Aarav Sharma", response.getCustomerName());
        assertEquals(new BigDecimal("100000.00"), response.getDisbursedAmount());
        assertTrue(response.getDisbursementReference().startsWith("DISB-"));

        assertEquals(ApplicationStatus.DISBURSED, app.getStatus());
        verify(disbursementDAO, times(1)).save(any(Disbursement.class));
        verify(loanApplicationDAO, times(1)).update(app);
        verify(historyDAO, times(1)).save(any());
    }
}
