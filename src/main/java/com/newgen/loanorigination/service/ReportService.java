package com.newgen.loanorigination.service;

import com.newgen.loanorigination.config.BusinessClock;
import com.newgen.loanorigination.dao.ReportDAO;
import com.newgen.loanorigination.dto.reports.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class ReportService {

    private final ReportDAO reportDAO;
    private final BusinessClock businessClock;

    public ReportService(ReportDAO reportDAO, BusinessClock businessClock) {
        this.reportDAO = reportDAO;
        this.businessClock = businessClock;
    }

    public List<ApplicationRegisterReportDTO> getApplicationRegister(String productType, String status) {
        return reportDAO.getApplicationRegister(productType, status);
    }

    public List<ProductPipelineStatusDTO> getProductPipelineByStatus() {
        return reportDAO.getProductPipelineByStatus();
    }

    public List<ProductPipelineBranchDTO> getProductPipelineByBranch() {
        return reportDAO.getProductPipelineByBranch();
    }

    public List<DisbursementSummaryDTO> getDisbursementSummary(LocalDate startDate, LocalDate endDate, String periodType) {
        return reportDAO.getDisbursementSummary(startDate, endDate, periodType);
    }

    public List<OverdueReviewReportDTO> getOverdueReviews() {
        return reportDAO.getOverdueReviews(businessClock.getCurrentDate());
    }

    public List<OverdueReviewReportDTO> getOverdueReviewsForDate(LocalDate date) {
        return reportDAO.getOverdueReviews(date);
    }

    public List<CustomerSummaryReportDTO> getCustomersWithoutDisbursement() {
        return reportDAO.getCustomersWithoutDisbursement();
    }

    public List<RepeatApplicantReportDTO> getRepeatApplicants() {
        return reportDAO.getRepeatApplicants();
    }
}
