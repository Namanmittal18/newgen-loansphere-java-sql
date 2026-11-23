package com.newgen.loanorigination.service;

import com.newgen.loanorigination.dto.EligibilityCalculationResult;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class FinancialCalculationService {

    public static final BigDecimal FOIR_MAX_LIMIT = new BigDecimal("50.00");
    private static final MathContext MC_PRECISE = new MathContext(16, RoundingMode.HALF_UP);

    /**
     * Calculates monthly EMI using the standard reducing balance formula:
     * EMI = P * r * (1+r)^n / ((1+r)^n - 1)
     *
     * @param principal requested amount (P)
     * @param annualInterestRate annual rate in percentage (e.g. 12 for 12%)
     * @param tenureMonths tenure in months (n)
     * @return rounded EMI to 2 decimal places with HALF_UP
     */
    public BigDecimal calculateEmi(BigDecimal principal, BigDecimal annualInterestRate, int tenureMonths) {
        if (principal == null || principal.compareTo(BigDecimal.ZERO) <= 0 || tenureMonths <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        if (annualInterestRate == null || annualInterestRate.compareTo(BigDecimal.ZERO) == 0) {
            // Zero interest rate: EMI = Principal / tenure
            return principal.divide(BigDecimal.valueOf(tenureMonths), 2, RoundingMode.HALF_UP);
        }

        // r = annual rate / 12 / 100
        BigDecimal monthlyRate = annualInterestRate.divide(BigDecimal.valueOf(1200), MC_PRECISE);

        // (1 + r)
        BigDecimal onePlusR = BigDecimal.ONE.add(monthlyRate, MC_PRECISE);

        // (1 + r)^n
        BigDecimal onePlusRPowerN = onePlusR.pow(tenureMonths, MC_PRECISE);

        // Numerator = P * r * (1+r)^n
        BigDecimal numerator = principal.multiply(monthlyRate, MC_PRECISE).multiply(onePlusRPowerN, MC_PRECISE);

        // Denominator = (1+r)^n - 1
        BigDecimal denominator = onePlusRPowerN.subtract(BigDecimal.ONE, MC_PRECISE);

        // EMI = Numerator / Denominator
        BigDecimal emi = numerator.divide(denominator, MC_PRECISE);

        return emi.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculates Fixed Obligation to Income Ratio (FOIR):
     * FOIR = (Existing Monthly Obligations + Rounded EMI) / Monthly Net Income * 100
     *
     * @param existingObligations existing monthly obligations
     * @param roundedEmi EMI rounded to 2 decimal places
     * @param monthlyNetIncome customer's net monthly income
     * @return FOIR rounded to 2 decimal places with HALF_UP
     */
    public BigDecimal calculateFoir(BigDecimal existingObligations, BigDecimal roundedEmi, BigDecimal monthlyNetIncome) {
        if (monthlyNetIncome == null || monthlyNetIncome.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Monthly net income must be strictly positive to calculate FOIR.");
        }

        BigDecimal obligations = (existingObligations != null) ? existingObligations : BigDecimal.ZERO;
        BigDecimal emi = (roundedEmi != null) ? roundedEmi : BigDecimal.ZERO;

        BigDecimal totalObligations = obligations.add(emi);

        BigDecimal foir = totalObligations
                .multiply(BigDecimal.valueOf(100), MC_PRECISE)
                .divide(monthlyNetIncome, MC_PRECISE);

        return foir.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Evaluates comprehensive financial eligibility based on calculated FOIR and preliminary rule failures.
     */
    public EligibilityCalculationResult evaluateEligibility(BigDecimal principal,
                                                             BigDecimal annualInterestRate,
                                                             int tenureMonths,
                                                             BigDecimal existingObligations,
                                                             BigDecimal monthlyNetIncome,
                                                             List<String> priorValidationFailures) {
        List<String> failureReasons = new ArrayList<>();
        if (priorValidationFailures != null) {
            failureReasons.addAll(priorValidationFailures);
        }

        BigDecimal emi = calculateEmi(principal, annualInterestRate, tenureMonths);
        BigDecimal foir = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        if (monthlyNetIncome != null && monthlyNetIncome.compareTo(BigDecimal.ZERO) > 0) {
            foir = calculateFoir(existingObligations, emi, monthlyNetIncome);
            if (foir.compareTo(FOIR_MAX_LIMIT) > 0) {
                failureReasons.add(String.format("FOIR percentage of %s%% exceeds maximum permissible limit of %s%%.",
                        foir.toPlainString(), FOIR_MAX_LIMIT.toPlainString()));
            }
        } else {
            failureReasons.add("Valid monthly net income is required for affordability assessment.");
        }

        boolean eligible = failureReasons.isEmpty();
        return new EligibilityCalculationResult(emi, foir, eligible, failureReasons);
    }
}
