package com.newgen.loanorigination;

import com.newgen.loanorigination.dto.EligibilityCalculationResult;
import com.newgen.loanorigination.service.FinancialCalculationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("1. Financial Calculation Engine Unit Tests")
class FinancialCalculationServiceTest {

    private FinancialCalculationService financialService;

    @BeforeEach
    void setUp() {
        financialService = new FinancialCalculationService();
    }

    @Test
    @DisplayName("Scenario 14: Worked EMI example matches formula exactly (₹1,00,000 @ 12% for 12 mos = ₹8,884.88)")
    void testWorkedEmiExample() {
        BigDecimal principal = new BigDecimal("100000.00");
        BigDecimal annualRate = new BigDecimal("12.00");
        int tenureMonths = 12;

        BigDecimal emi = financialService.calculateEmi(principal, annualRate, tenureMonths);

        assertNotNull(emi);
        assertEquals(new BigDecimal("8884.88"), emi, "Expected EMI to match ₹8,884.88 exactly");
    }

    @Test
    @DisplayName("Scenario 15: FOIR exactly 50.00% is eligible")
    void testFoirExactly50Percent() {
        // Income = ₹30,000; Total obligations (existing + EMI) = ₹15,000 -> FOIR = 50.00%
        BigDecimal income = new BigDecimal("30000.00");
        BigDecimal existingObligations = new BigDecimal("6115.12");
        BigDecimal roundedEmi = new BigDecimal("8884.88"); // sum = 15,000.00

        BigDecimal foir = financialService.calculateFoir(existingObligations, roundedEmi, income);
        assertEquals(new BigDecimal("50.00"), foir);

        EligibilityCalculationResult result = financialService.evaluateEligibility(
                new BigDecimal("100000.00"),
                new BigDecimal("12.00"),
                12,
                existingObligations,
                income,
                List.of()
        );

        assertTrue(result.isEligible(), "FOIR of exactly 50.00% must be eligible");
        assertTrue(result.getFailureReasons().isEmpty());
    }

    @Test
    @DisplayName("Scenario 16: FOIR above 50.00% (e.g. 52.95%) is ineligible")
    void testFoirAbove50Percent() {
        // Income = ₹30,000, obligations = ₹7,000, EMI = ₹8,884.88 -> Total = ₹15,884.88 -> FOIR = 52.95%
        BigDecimal income = new BigDecimal("30000.00");
        BigDecimal existingObligations = new BigDecimal("7000.00");
        BigDecimal roundedEmi = new BigDecimal("8884.88");

        BigDecimal foir = financialService.calculateFoir(existingObligations, roundedEmi, income);
        assertEquals(new BigDecimal("52.95"), foir);

        EligibilityCalculationResult result = financialService.evaluateEligibility(
                new BigDecimal("100000.00"),
                new BigDecimal("12.00"),
                12,
                existingObligations,
                income,
                List.of()
        );

        assertFalse(result.isEligible(), "FOIR > 50.00% must be ineligible");
        assertFalse(result.getFailureReasons().isEmpty());
        assertTrue(result.getFailureReasons().get(0).contains("exceeds maximum permissible limit"));
    }

    @Test
    @DisplayName("Zero interest rate calculation handled safely (P / n)")
    void testZeroInterestRate() {
        BigDecimal principal = new BigDecimal("120000.00");
        BigDecimal annualRate = BigDecimal.ZERO;
        int tenureMonths = 12;

        BigDecimal emi = financialService.calculateEmi(principal, annualRate, tenureMonths);
        assertEquals(new BigDecimal("10000.00"), emi);
    }

    @Test
    @DisplayName("Regression Test: Currency formatting across boundary values (500, 5000, 50000, 500000)")
    void testCurrencyFormattingIndianNumbering() {
        assertEquals("₹500.00", com.newgen.loanorigination.utility.CurrencyFormatter.formatInr(new BigDecimal("500.00")));
        assertEquals("₹5,000.00", com.newgen.loanorigination.utility.CurrencyFormatter.formatInr(new BigDecimal("5000.00")),
                "Must format as ₹5,000.00 and never ₹5,00,0.00");
        assertEquals("5,000.00", com.newgen.loanorigination.utility.CurrencyFormatter.formatIndianNumberOnly(new BigDecimal("5000.00")));
        assertEquals("₹50,000.00", com.newgen.loanorigination.utility.CurrencyFormatter.formatInr(new BigDecimal("50000.00")));
        assertEquals("₹5,00,000.00", com.newgen.loanorigination.utility.CurrencyFormatter.formatInr(new BigDecimal("500000.00")));
        assertEquals("₹50,00,000.00", com.newgen.loanorigination.utility.CurrencyFormatter.formatInr(new BigDecimal("5000000.00")));
    }
}
