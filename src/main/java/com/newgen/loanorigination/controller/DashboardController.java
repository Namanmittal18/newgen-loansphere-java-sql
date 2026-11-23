package com.newgen.loanorigination.controller;

import com.newgen.loanorigination.dao.DisbursementDAO;
import com.newgen.loanorigination.dao.LoanApplicationDAO;
import com.newgen.loanorigination.model.LoanApplication;
import com.newgen.loanorigination.model.enums.ApplicationStatus;
import com.newgen.loanorigination.model.enums.ProductType;
import com.newgen.loanorigination.service.CustomerService;
import com.newgen.loanorigination.service.LoanApplicationService;
import com.newgen.loanorigination.service.ReportService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class DashboardController {

    private final CustomerService customerService;
    private final LoanApplicationService loanApplicationService;
    private final LoanApplicationDAO loanApplicationDAO;
    private final DisbursementDAO disbursementDAO;
    private final ReportService reportService;

    public DashboardController(CustomerService customerService,
                               LoanApplicationService loanApplicationService,
                               LoanApplicationDAO loanApplicationDAO,
                               DisbursementDAO disbursementDAO,
                               ReportService reportService) {
        this.customerService = customerService;
        this.loanApplicationService = loanApplicationService;
        this.loanApplicationDAO = loanApplicationDAO;
        this.disbursementDAO = disbursementDAO;
        this.reportService = reportService;
    }

    @GetMapping({"/", "/dashboard"})
    public String showDashboard(Model model) {
        long totalCustomers = customerService.getTotalCustomersCount();
        long totalApplications = loanApplicationDAO.countTotal();

        long draftCount = loanApplicationDAO.countByStatus(ApplicationStatus.DRAFT);
        long underReviewCount = loanApplicationDAO.countByStatus(ApplicationStatus.UNDER_REVIEW);
        long creditApprovalCount = loanApplicationDAO.countByStatus(ApplicationStatus.CREDIT_APPROVAL);
        long rejectedCount = loanApplicationDAO.countByStatus(ApplicationStatus.REJECTED);
        long disbursedCount = loanApplicationDAO.countByStatus(ApplicationStatus.DISBURSED);
        long exitCount = loanApplicationDAO.countByStatus(ApplicationStatus.EXIT);

        int overdueCount = reportService.getOverdueReviews().size();
        BigDecimal totalDisbursedAmount = disbursementDAO.sumTotalDisbursedAmount();

        List<LoanApplication> recentApplications = loanApplicationService.getAllApplications();
        if (recentApplications.size() > 8) {
            recentApplications = recentApplications.subList(0, 8);
        }

        model.addAttribute("totalCustomers", totalCustomers);
        model.addAttribute("totalApplications", totalApplications);
        model.addAttribute("draftCount", draftCount);
        model.addAttribute("underReviewCount", underReviewCount);
        model.addAttribute("creditApprovalCount", creditApprovalCount);
        model.addAttribute("rejectedCount", rejectedCount);
        model.addAttribute("disbursedCount", disbursedCount + exitCount);
        model.addAttribute("overdueCount", overdueCount);
        model.addAttribute("totalDisbursedAmount", totalDisbursedAmount);
        model.addAttribute("recentApplications", recentApplications);

        return "dashboard";
    }
}
