package com.newgen.loanorigination.exception;

import com.newgen.loanorigination.model.enums.ApplicationStatus;

public class InvalidApplicationStateException extends RuntimeException {
    public InvalidApplicationStateException(ApplicationStatus current, ApplicationStatus target) {
        super(String.format("Invalid application state transition from %s to %s", current, target));
    }

    public InvalidApplicationStateException(String message) {
        super(message);
    }
}
