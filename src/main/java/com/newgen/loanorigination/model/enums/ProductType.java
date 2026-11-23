package com.newgen.loanorigination.model.enums;

public enum ProductType {
    PERSONAL("Personal Loan"),
    EDUCATION("Education Loan");

    private final String displayName;

    ProductType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
