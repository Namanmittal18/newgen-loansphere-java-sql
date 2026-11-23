package com.newgen.loanorigination.dao;

import com.newgen.loanorigination.dto.reports.*;

import java.time.LocalDate;
import java.util.List;

public interface ReportDAO {
    List<ApplicationRegisterReportDTO> getApplicationRegister(String productType, String status);
    List<ProductPipelineStatusDTO> getProductPipelineByStatus();
    List<ProductPipelineBranchDTO> getProductPipelineByBranch();
    List<DisbursementSummaryDTO> getDisbursementSummary(LocalDate startDate, LocalDate endDate, String periodType);
    List<OverdueReviewReportDTO> getOverdueReviews(LocalDate businessDate);
    List<CustomerSummaryReportDTO> getCustomersWithoutDisbursement();
    List<RepeatApplicantReportDTO> getRepeatApplicants();
}
