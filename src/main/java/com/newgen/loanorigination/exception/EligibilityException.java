package com.newgen.loanorigination.exception;

import java.util.List;

public class EligibilityException extends RuntimeException {
    private final List<String> reasons;

    public EligibilityException(String message, List<String> reasons) {
        super(message);
        this.reasons = reasons;
    }

    public List<String> getReasons() {
        return reasons;
    }
}
