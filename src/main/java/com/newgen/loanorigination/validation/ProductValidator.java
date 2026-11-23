package com.newgen.loanorigination.validation;

import com.newgen.loanorigination.config.LoanProductConfig;
import com.newgen.loanorigination.config.LoanProductConfig.ProductRule;
import com.newgen.loanorigination.model.LoanApplication;
import com.newgen.loanorigination.model.enums.ProductType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class ProductValidator {

    private final LoanProductConfig productConfig;

    public ProductValidator(LoanProductConfig productConfig) {
        this.productConfig = productConfig;
    }

    public List<String> validateRules(LoanApplication application) {
        List<String> failedRules = new ArrayList<>();

        if (application == null) {
            failedRules.add("Loan application details are missing.");
            return failedRules;
        }

        ProductType productType = application.getProductType();
        if (productType == null) {
            failedRules.add("Product type is required (PERSONAL or EDUCATION).");
            return failedRules;
        }

        ProductRule rule = productConfig.getRule(productType);

        // Requested amount validation
        BigDecimal amount = application.getRequestedAmount();
        if (amount == null) {
            failedRules.add("Requested amount is required.");
        } else {
            if (amount.compareTo(rule.getMinAmount()) < 0 || amount.compareTo(rule.getMaxAmount()) > 0) {
                failedRules.add(String.format("Requested amount (₹%s) must be between ₹%s and ₹%s for %s product.",
                        amount.toPlainString(), rule.getMinAmount().toPlainString(), rule.getMaxAmount().toPlainString(), productType));
            }
        }

        // Tenure validation
        Integer tenure = application.getTenureMonths();
        if (tenure == null) {
            failedRules.add("Tenure in months is required.");
        } else {
            if (tenure < rule.getMinTenure() || tenure > rule.getMaxTenure()) {
                failedRules.add(String.format("Tenure (%d months) must be between %d and %d months for %s product.",
                        tenure, rule.getMinTenure(), rule.getMaxTenure(), productType));
            }
        }

        // Monthly net income validation
        BigDecimal income = application.getMonthlyNetIncome();
        if (income == null) {
            failedRules.add("Monthly net income is required.");
        } else {
            if (income.compareTo(BigDecimal.ZERO) <= 0) {
                failedRules.add("Monthly net income must be strictly positive.");
            } else if (income.compareTo(rule.getMinIncome()) < 0) {
                failedRules.add(String.format("Monthly net income (₹%s) must be at least ₹%s for %s product.",
                        income.toPlainString(), rule.getMinIncome().toPlainString(), productType));
            }
        }

        // Existing monthly obligations validation
        BigDecimal obligations = application.getExistingMonthlyObligations();
        if (obligations == null) {
            failedRules.add("Existing monthly obligations are required.");
        } else if (obligations.compareTo(BigDecimal.ZERO) < 0) {
            failedRules.add("Existing monthly obligations cannot be negative.");
        }

        return failedRules;
    }
}
