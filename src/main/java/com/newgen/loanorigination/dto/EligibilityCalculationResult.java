package com.newgen.loanorigination.dto;

import java.math.BigDecimal;
import java.util.List;

public class EligibilityCalculationResult {
    private final BigDecimal proposedEmi;
    private final BigDecimal foirPercentage;
    private final boolean eligible;
    private final List<String> failureReasons;

    public EligibilityCalculationResult(BigDecimal proposedEmi, BigDecimal foirPercentage,
                                        boolean eligible, List<String> failureReasons) {
        this.proposedEmi = proposedEmi;
        this.foirPercentage = foirPercentage;
        this.eligible = eligible;
        this.failureReasons = failureReasons;
    }

    public BigDecimal getProposedEmi() {
        return proposedEmi;
    }

    public BigDecimal getFoirPercentage() {
        return foirPercentage;
    }

    public boolean isEligible() {
        return eligible;
    }

    public List<String> getFailureReasons() {
        return failureReasons;
    }
}
