package com.newgen.loanorigination;

import com.newgen.loanorigination.config.LoanProductConfig;
import com.newgen.loanorigination.model.LoanApplication;
import com.newgen.loanorigination.model.enums.ProductType;
import com.newgen.loanorigination.validation.ProductValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("3. Product Rules & Boundary Tests")
class ProductValidatorTest {

    private ProductValidator validator;

    @BeforeEach
    void setUp() {
        LoanProductConfig config = new LoanProductConfig();
        validator = new ProductValidator(config);
    }

    private LoanApplication createSampleApp(ProductType type, BigDecimal amount, int tenure,
                                            BigDecimal income, BigDecimal obligations) {
        LoanApplication app = new LoanApplication();
        app.setProductType(type);
        app.setRequestedAmount(amount);
        app.setTenureMonths(tenure);
        app.setMonthlyNetIncome(income);
        app.setExistingMonthlyObligations(obligations);
        return app;
    }

    @Test
    @DisplayName("Scenario 1 & 2: PERSONAL loan amount boundaries (₹50,000 to ₹5,00,000 inclusive)")
    void testPersonalAmountBoundaries() {
        // Minimum boundary
        LoanApplication minApp = createSampleApp(ProductType.PERSONAL, new BigDecimal("50000.00"), 12, new BigDecimal("25000.00"), BigDecimal.ZERO);
        assertTrue(validator.validateRules(minApp).isEmpty(), "₹50,000 should be valid for PERSONAL");

        LoanApplication belowMin = createSampleApp(ProductType.PERSONAL, new BigDecimal("49999.00"), 12, new BigDecimal("25000.00"), BigDecimal.ZERO);
        List<String> belowErrors = validator.validateRules(belowMin);
        assertFalse(belowErrors.isEmpty(), "₹49,999 should be rejected for PERSONAL");

        // Maximum boundary
        LoanApplication maxApp = createSampleApp(ProductType.PERSONAL, new BigDecimal("500000.00"), 60, new BigDecimal("40000.00"), BigDecimal.ZERO);
        assertTrue(validator.validateRules(maxApp).isEmpty(), "₹5,00,000 should be valid for PERSONAL");

        LoanApplication aboveMax = createSampleApp(ProductType.PERSONAL, new BigDecimal("500001.00"), 60, new BigDecimal("40000.00"), BigDecimal.ZERO);
        assertFalse(validator.validateRules(aboveMax).isEmpty(), "₹5,00,001 should be rejected for PERSONAL");
    }

    @Test
    @DisplayName("Scenario 3 & 4: EDUCATION loan amount boundaries (₹1,00,000 to ₹10,00,000 inclusive)")
    void testEducationAmountBoundaries() {
        // Minimum boundary
        LoanApplication minApp = createSampleApp(ProductType.EDUCATION, new BigDecimal("100000.00"), 12, new BigDecimal("20000.00"), BigDecimal.ZERO);
        assertTrue(validator.validateRules(minApp).isEmpty(), "₹1,00,000 should be valid for EDUCATION");

        LoanApplication belowMin = createSampleApp(ProductType.EDUCATION, new BigDecimal("99999.00"), 12, new BigDecimal("20000.00"), BigDecimal.ZERO);
        assertFalse(validator.validateRules(belowMin).isEmpty(), "₹99,999 should be rejected for EDUCATION");

        // Maximum boundary
        LoanApplication maxApp = createSampleApp(ProductType.EDUCATION, new BigDecimal("1000000.00"), 84, new BigDecimal("50000.00"), BigDecimal.ZERO);
        assertTrue(validator.validateRules(maxApp).isEmpty(), "₹10,00,000 should be valid for EDUCATION");

        LoanApplication aboveMax = createSampleApp(ProductType.EDUCATION, new BigDecimal("1000001.00"), 84, new BigDecimal("50000.00"), BigDecimal.ZERO);
        assertFalse(validator.validateRules(aboveMax).isEmpty(), "₹10,00,001 should be rejected for EDUCATION");
    }

    @Test
    @DisplayName("Scenario 5: Minimum and Maximum tenure boundaries")
    void testTenureBoundaries() {
        // PERSONAL: 12 to 60 months
        LoanApplication pValidMin = createSampleApp(ProductType.PERSONAL, new BigDecimal("100000.00"), 12, new BigDecimal("25000.00"), BigDecimal.ZERO);
        assertTrue(validator.validateRules(pValidMin).isEmpty());

        LoanApplication pInvalidMin = createSampleApp(ProductType.PERSONAL, new BigDecimal("100000.00"), 11, new BigDecimal("25000.00"), BigDecimal.ZERO);
        assertFalse(validator.validateRules(pInvalidMin).isEmpty());

        LoanApplication pValidMax = createSampleApp(ProductType.PERSONAL, new BigDecimal("100000.00"), 60, new BigDecimal("25000.00"), BigDecimal.ZERO);
        assertTrue(validator.validateRules(pValidMax).isEmpty());

        LoanApplication pInvalidMax = createSampleApp(ProductType.PERSONAL, new BigDecimal("100000.00"), 61, new BigDecimal("25000.00"), BigDecimal.ZERO);
        assertFalse(validator.validateRules(pInvalidMax).isEmpty());

        // EDUCATION: 12 to 84 months
        LoanApplication eValidMax = createSampleApp(ProductType.EDUCATION, new BigDecimal("200000.00"), 84, new BigDecimal("25000.00"), BigDecimal.ZERO);
        assertTrue(validator.validateRules(eValidMax).isEmpty());

        LoanApplication eInvalidMax = createSampleApp(ProductType.EDUCATION, new BigDecimal("200000.00"), 85, new BigDecimal("25000.00"), BigDecimal.ZERO);
        assertFalse(validator.validateRules(eInvalidMax).isEmpty());
    }

    @Test
    @DisplayName("Scenario 17: Negative monthly obligations are rejected")
    void testNegativeObligationsRejected() {
        LoanApplication app = createSampleApp(ProductType.PERSONAL, new BigDecimal("100000.00"), 12,
                new BigDecimal("25000.00"), new BigDecimal("-500.00"));
        List<String> errors = validator.validateRules(app);
        assertFalse(errors.isEmpty());
        assertTrue(errors.get(0).contains("cannot be negative"));
    }

    @Test
    @DisplayName("Scenario 18: Zero or negative income is rejected")
    void testZeroOrNegativeIncomeRejected() {
        LoanApplication zeroApp = createSampleApp(ProductType.PERSONAL, new BigDecimal("100000.00"), 12,
                BigDecimal.ZERO, BigDecimal.ZERO);
        List<String> zeroErrors = validator.validateRules(zeroApp);
        assertFalse(zeroErrors.isEmpty());

        LoanApplication negApp = createSampleApp(ProductType.PERSONAL, new BigDecimal("100000.00"), 12,
                new BigDecimal("-1000.00"), BigDecimal.ZERO);
        List<String> negErrors = validator.validateRules(negApp);
        assertFalse(negErrors.isEmpty());
    }
}
