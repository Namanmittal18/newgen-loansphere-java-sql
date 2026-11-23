package com.newgen.loanorigination.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class DisbursementResponse {
    private final String applicationNumber;
    private final String customerName;
    private final BigDecimal disbursedAmount;
    private final String disbursementReference;
    private final LocalDateTime disbursedDate;

    public DisbursementResponse(String applicationNumber, String customerName, BigDecimal disbursedAmount,
                                String disbursementReference, LocalDateTime disbursedDate) {
        this.applicationNumber = applicationNumber;
        this.customerName = customerName;
        this.disbursedAmount = disbursedAmount;
        this.disbursementReference = disbursementReference;
        this.disbursedDate = disbursedDate;
    }

    public String getApplicationNumber() {
        return applicationNumber;
    }

    public String getCustomerName() {
        return customerName;
    }

    public BigDecimal getDisbursedAmount() {
        return disbursedAmount;
    }

    public String getDisbursementReference() {
        return disbursementReference;
    }

    public LocalDateTime getDisbursedDate() {
        return disbursedDate;
    }
}
