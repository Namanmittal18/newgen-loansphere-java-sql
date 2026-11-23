package com.newgen.loanorigination.model.enums;

public enum ApplicationStatus {
    DRAFT,
    UNDER_REVIEW,
    CREDIT_APPROVAL,
    REJECTED,
    DISBURSED,
    CORE_BANKING_PUSH,
    EXIT;

    public boolean isActive() {
        return this == DRAFT || this == UNDER_REVIEW || this == CREDIT_APPROVAL;
    }

    public boolean isTerminal() {
        return this == REJECTED || this == EXIT;
    }
}
