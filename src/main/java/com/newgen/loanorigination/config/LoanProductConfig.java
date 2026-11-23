package com.newgen.loanorigination.config;

import com.newgen.loanorigination.model.enums.ProductType;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "loansphere.product")
public class LoanProductConfig {

    public static class ProductRule {
        private BigDecimal minAmount;
        private BigDecimal maxAmount;
        private Integer minTenure;
        private Integer maxTenure;
        private BigDecimal minIncome;
        private BigDecimal annualInterestRate;
        private boolean allowCoBorrower;

        public ProductRule() {
        }

        public ProductRule(BigDecimal minAmount, BigDecimal maxAmount, Integer minTenure,
                           Integer maxTenure, BigDecimal minIncome, BigDecimal annualInterestRate,
                           boolean allowCoBorrower) {
            this.minAmount = minAmount;
            this.maxAmount = maxAmount;
            this.minTenure = minTenure;
            this.maxTenure = maxTenure;
            this.minIncome = minIncome;
            this.annualInterestRate = annualInterestRate;
            this.allowCoBorrower = allowCoBorrower;
        }

        public BigDecimal getMinAmount() {
            return minAmount;
        }

        public void setMinAmount(BigDecimal minAmount) {
            this.minAmount = minAmount;
        }

        public BigDecimal getMaxAmount() {
            return maxAmount;
        }

        public void setMaxAmount(BigDecimal maxAmount) {
            this.maxAmount = maxAmount;
        }

        public Integer getMinTenure() {
            return minTenure;
        }

        public void setMinTenure(Integer minTenure) {
            this.minTenure = minTenure;
        }

        public Integer getMaxTenure() {
            return maxTenure;
        }

        public void setMaxTenure(Integer maxTenure) {
            this.maxTenure = maxTenure;
        }

        public BigDecimal getMinIncome() {
            return minIncome;
        }

        public void setMinIncome(BigDecimal minIncome) {
            this.minIncome = minIncome;
        }

        public BigDecimal getAnnualInterestRate() {
            return annualInterestRate;
        }

        public void setAnnualInterestRate(BigDecimal annualInterestRate) {
            this.annualInterestRate = annualInterestRate;
        }

        public boolean isAllowCoBorrower() {
            return allowCoBorrower;
        }

        public void setAllowCoBorrower(boolean allowCoBorrower) {
            this.allowCoBorrower = allowCoBorrower;
        }
    }

    private ProductRule personal = new ProductRule(
            new BigDecimal("50000.00"),
            new BigDecimal("500000.00"),
            12,
            60,
            new BigDecimal("20000.00"),
            new BigDecimal("12.00"),
            true
    );

    private ProductRule education = new ProductRule(
            new BigDecimal("100000.00"),
            new BigDecimal("1000000.00"),
            12,
            84,
            new BigDecimal("15000.00"),
            new BigDecimal("10.00"),
            false // No co-borrower allowed per assignment rule
    );

    public ProductRule getPersonal() {
        return personal;
    }

    public void setPersonal(ProductRule personal) {
        this.personal = personal;
    }

    public ProductRule getEducation() {
        return education;
    }

    public void setEducation(ProductRule education) {
        this.education = education;
    }

    public ProductRule getRule(ProductType productType) {
        if (productType == ProductType.PERSONAL) {
            return personal;
        } else if (productType == ProductType.EDUCATION) {
            return education;
        }
        throw new IllegalArgumentException("Unsupported product type: " + productType);
    }
}
