package com.newgen.loanorigination;

import com.newgen.loanorigination.config.BusinessClock;
import com.newgen.loanorigination.config.LoanProductConfig;
import com.newgen.loanorigination.dao.ApplicationHistoryDAO;
import com.newgen.loanorigination.dao.CustomerDAO;
import com.newgen.loanorigination.dao.LoanApplicationDAO;
import com.newgen.loanorigination.dto.ApplicationSubmissionResponse;
import com.newgen.loanorigination.exception.DuplicateApplicationException;
import com.newgen.loanorigination.model.Customer;
import com.newgen.loanorigination.model.LoanApplication;
import com.newgen.loanorigination.model.enums.ApplicationStatus;
import com.newgen.loanorigination.model.enums.ProductType;
import com.newgen.loanorigination.service.FinancialCalculationService;
import com.newgen.loanorigination.service.LoanApplicationService;
import com.newgen.loanorigination.validation.CustomerValidator;
import com.newgen.loanorigination.validation.ProductValidator;
import com.newgen.loanorigination.validation.WorkflowValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("5. Loan Application Workflow & Service Tests")
class LoanApplicationServiceTest {

    private LoanApplicationDAO loanApplicationDAO;
    private CustomerDAO customerDAO;
    private ApplicationHistoryDAO historyDAO;
    private LoanApplicationService loanApplicationService;
    private BusinessClock businessClock;

    @BeforeEach
    void setUp() {
        loanApplicationDAO = mock(LoanApplicationDAO.class);
        customerDAO = mock(CustomerDAO.class);
        historyDAO = mock(ApplicationHistoryDAO.class);

        businessClock = new BusinessClock("2026-11-23");
        FinancialCalculationService finService = new FinancialCalculationService();
        CustomerValidator custValidator = new CustomerValidator();
        LoanProductConfig productConfig = new LoanProductConfig();
        ProductValidator prodValidator = new ProductValidator(productConfig);
        WorkflowValidator workflowValidator = new WorkflowValidator();

        loanApplicationService = new LoanApplicationService(
                loanApplicationDAO,
                customerDAO,
                historyDAO,
                finService,
                custValidator,
                prodValidator,
                workflowValidator,
                productConfig,
                businessClock
        );
    }

    private Customer createMockCustomer(Long id) {
        Customer c = new Customer();
        c.setCustomerId(id);
        c.setCustomerCode("CUST-20260101-0001");
        c.setFullName("Aarav Sharma");
        c.setDateOfBirth(LocalDate.of(1995, 5, 15)); // Age ~31
        c.setMobileNumber("9876543210");
        return c;
    }

    @Test
    @DisplayName("Scenario 12: Duplicate active application for same customer and product is rejected")
    void testDuplicateActiveApplicationRejected() {
        Customer customer = createMockCustomer(1L);
        when(customerDAO.findById(1L)).thenReturn(Optional.of(customer));
        // Customer already has active PERSONAL application
        when(loanApplicationDAO.hasActiveApplication(1L, ProductType.PERSONAL)).thenReturn(true);

        LoanApplication newApp = new LoanApplication();
        newApp.setCustomerId(1L);
        newApp.setProductType(ProductType.PERSONAL);

        assertThrows(DuplicateApplicationException.class, () ->
                loanApplicationService.createDraft(newApp));
    }

    @Test
    @DisplayName("Scenario 13: New application allowed after previous application was REJECTED or EXIT")
    void testNewApplicationAllowedAfterRejectionOrExit() {
        Customer customer = createMockCustomer(1L);
        when(customerDAO.findById(1L)).thenReturn(Optional.of(customer));
        // No active applications exist (previous were rejected or completed/exited)
        when(loanApplicationDAO.hasActiveApplication(1L, ProductType.PERSONAL)).thenReturn(false);
        when(loanApplicationDAO.save(any(LoanApplication.class))).thenAnswer(i -> {
            LoanApplication a = i.getArgument(0);
            a.setApplicationId(100L);
            return a;
        });

        LoanApplication newApp = new LoanApplication();
        newApp.setCustomerId(1L);
        newApp.setProductType(ProductType.PERSONAL);
        newApp.setRequestedAmount(new BigDecimal("100000.00"));
        newApp.setTenureMonths(12);

        LoanApplication saved = loanApplicationService.createDraft(newApp);
        assertNotNull(saved);
        assertEquals(ApplicationStatus.DRAFT, saved.getStatus());
        verify(historyDAO, times(1)).save(any());
    }

    @Test
    @DisplayName("Application submission: eligible application transitions to UNDER_REVIEW")
    void testSubmissionEligible() {
        Customer customer = createMockCustomer(1L);
        when(customerDAO.findById(1L)).thenReturn(Optional.of(customer));

        LoanApplication app = new LoanApplication();
        app.setApplicationId(10L);
        app.setApplicationNumber("APP-20261123-0001");
        app.setCustomerId(1L);
        app.setProductType(ProductType.PERSONAL);
        app.setRequestedAmount(new BigDecimal("100000.00"));
        app.setTenureMonths(12);
        app.setMonthlyNetIncome(new BigDecimal("30000.00"));
        app.setExistingMonthlyObligations(new BigDecimal("5000.00")); // FOIR ~46.28%
        app.setStatus(ApplicationStatus.DRAFT);
        app.setCreatedAt(LocalDateTime.now());

        when(loanApplicationDAO.findById(10L)).thenReturn(Optional.of(app));

        ApplicationSubmissionResponse response = loanApplicationService.submitApplication(10L);

        assertTrue(response.isEligible());
        assertEquals(ApplicationStatus.UNDER_REVIEW, response.getStatus());
        assertEquals(new BigDecimal("8884.88"), response.getProposedEmi());
        assertEquals(new BigDecimal("46.28"), response.getFoirPercentage());
        verify(loanApplicationDAO, times(1)).update(app);
        verify(historyDAO, times(1)).save(any());
    }

    @Test
    @DisplayName("Application submission: ineligible application (FOIR > 50%) transitions to REJECTED")
    void testSubmissionIneligible() {
        Customer customer = createMockCustomer(1L);
        when(customerDAO.findById(1L)).thenReturn(Optional.of(customer));

        LoanApplication app = new LoanApplication();
        app.setApplicationId(11L);
        app.setApplicationNumber("APP-20261123-0002");
        app.setCustomerId(1L);
        app.setProductType(ProductType.PERSONAL);
        app.setRequestedAmount(new BigDecimal("100000.00"));
        app.setTenureMonths(12);
        app.setMonthlyNetIncome(new BigDecimal("30000.00"));
        app.setExistingMonthlyObligations(new BigDecimal("7000.00")); // FOIR ~52.95%
        app.setStatus(ApplicationStatus.DRAFT);
        app.setCreatedAt(LocalDateTime.now());

        when(loanApplicationDAO.findById(11L)).thenReturn(Optional.of(app));

        ApplicationSubmissionResponse response = loanApplicationService.submitApplication(11L);

        assertFalse(response.isEligible());
        assertEquals(ApplicationStatus.REJECTED, response.getStatus());
        assertEquals(new BigDecimal("52.95"), response.getFoirPercentage());
        assertFalse(response.getFailedReasons().isEmpty());
        verify(loanApplicationDAO, times(1)).update(app);
        verify(historyDAO, times(1)).save(any());
    }
}
