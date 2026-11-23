package com.newgen.loanorigination.dao;

import com.newgen.loanorigination.model.LoanApplication;
import com.newgen.loanorigination.model.enums.ApplicationStatus;
import com.newgen.loanorigination.model.enums.ProductType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface LoanApplicationDAO {
    LoanApplication save(LoanApplication application);
    void update(LoanApplication application);
    void updateStatus(Long applicationId, ApplicationStatus status);
    Optional<LoanApplication> findById(Long applicationId);
    Optional<LoanApplication> findByApplicationNumber(String applicationNumber);
    List<LoanApplication> findByCustomerId(Long customerId);
    List<LoanApplication> findByStatus(ApplicationStatus status);
    List<LoanApplication> findAll();
    boolean hasActiveApplication(Long customerId, ProductType productType);
    List<LoanApplication> findOverdueUnderReview(LocalDate businessDate, int thresholdDays);
    List<LoanApplication> findDisbursedPendingPush();
    long countByStatus(ApplicationStatus status);
    long countTotal();
}
