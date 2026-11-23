package com.newgen.loanorigination.controller;

import com.newgen.loanorigination.dto.reports.*;
import com.newgen.loanorigination.service.ReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping
    public String showReports(@RequestParam(value = "tab", defaultValue = "register") String tab,
                              @RequestParam(value = "productType", required = false) String productType,
                              @RequestParam(value = "status", required = false) String status,
                              @RequestParam(value = "periodType", defaultValue = "DAILY") String periodType,
                              @RequestParam(value = "businessDate", required = false)
                              @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate businessDate,
                              Model model) {

        model.addAttribute("activeTab", tab);

        // Report 1: Application Register
        List<ApplicationRegisterReportDTO> registerReport = reportService.getApplicationRegister(productType, status);
        model.addAttribute("registerReport", registerReport);
        model.addAttribute("selectedProduct", productType);
        model.addAttribute("selectedStatus", status);

        // Report 2: Product Pipeline
        model.addAttribute("pipelineByStatus", reportService.getProductPipelineByStatus());
        model.addAttribute("pipelineByBranch", reportService.getProductPipelineByBranch());

        // Report 3: Disbursement Summary
        model.addAttribute("disbursementSummary", reportService.getDisbursementSummary(null, null, periodType));
        model.addAttribute("selectedPeriodType", periodType);

        // Report 4: Overdue Reviews
        List<OverdueReviewReportDTO> overdueReviews = (businessDate != null)
                ? reportService.getOverdueReviewsForDate(businessDate)
                : reportService.getOverdueReviews();
        model.addAttribute("overdueReviews", overdueReviews);
        model.addAttribute("filterBusinessDate", businessDate);

        // Report 5: Customers Without Disbursement
        model.addAttribute("customersWithoutDisbursement", reportService.getCustomersWithoutDisbursement());

        // Report 6: Repeat Applicants
        model.addAttribute("repeatApplicants", reportService.getRepeatApplicants());

        return "reports/index";
    }
}
