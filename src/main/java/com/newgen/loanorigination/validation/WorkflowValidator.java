package com.newgen.loanorigination.validation;

import com.newgen.loanorigination.exception.InvalidApplicationStateException;
import com.newgen.loanorigination.exception.ValidationException;
import com.newgen.loanorigination.model.enums.ApplicationStatus;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Component
public class WorkflowValidator {

    private static final Map<ApplicationStatus, Set<ApplicationStatus>> ALLOWED_TRANSITIONS = new EnumMap<>(ApplicationStatus.class);

    static {
        ALLOWED_TRANSITIONS.put(ApplicationStatus.DRAFT,
                EnumSet.of(ApplicationStatus.UNDER_REVIEW, ApplicationStatus.REJECTED));

        ALLOWED_TRANSITIONS.put(ApplicationStatus.UNDER_REVIEW,
                EnumSet.of(ApplicationStatus.CREDIT_APPROVAL, ApplicationStatus.REJECTED));

        ALLOWED_TRANSITIONS.put(ApplicationStatus.CREDIT_APPROVAL,
                EnumSet.of(ApplicationStatus.DISBURSED, ApplicationStatus.REJECTED));

        ALLOWED_TRANSITIONS.put(ApplicationStatus.DISBURSED,
                EnumSet.of(ApplicationStatus.CORE_BANKING_PUSH));

        ALLOWED_TRANSITIONS.put(ApplicationStatus.CORE_BANKING_PUSH,
                EnumSet.of(ApplicationStatus.EXIT));

        ALLOWED_TRANSITIONS.put(ApplicationStatus.REJECTED, EnumSet.noneOf(ApplicationStatus.class));
        ALLOWED_TRANSITIONS.put(ApplicationStatus.EXIT, EnumSet.noneOf(ApplicationStatus.class));
    }

    public void validateTransition(ApplicationStatus current, ApplicationStatus target) {
        if (current == null || target == null) {
            throw new InvalidApplicationStateException("Application statuses cannot be null.");
        }

        Set<ApplicationStatus> allowed = ALLOWED_TRANSITIONS.get(current);
        if (allowed == null || !allowed.contains(target)) {
            throw new InvalidApplicationStateException(current, target);
        }
    }

    public void validateRejectionReason(ApplicationStatus targetStatus, String reason) {
        if (targetStatus == ApplicationStatus.REJECTED) {
            if (reason == null || reason.trim().isEmpty()) {
                throw new ValidationException("Rejection action requires a mandatory, non-empty reason.");
            }
        }
    }
}
