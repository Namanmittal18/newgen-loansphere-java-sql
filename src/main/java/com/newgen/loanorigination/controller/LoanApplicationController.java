package com.newgen.loanorigination.controller;

import com.newgen.loanorigination.dto.ApplicationSubmissionResponse;
import com.newgen.loanorigination.model.ApplicationHistory;
import com.newgen.loanorigination.model.Customer;
import com.newgen.loanorigination.model.LoanApplication;
import com.newgen.loanorigination.model.enums.ApplicationStatus;
import com.newgen.loanorigination.model.enums.ProductType;
import com.newgen.loanorigination.service.CustomerService;
import com.newgen.loanorigination.service.LoanApplicationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/applications")
public class LoanApplicationController {

    private final LoanApplicationService loanApplicationService;
    private final CustomerService customerService;

    public LoanApplicationController(LoanApplicationService loanApplicationService, CustomerService customerService) {
        this.loanApplicationService = loanApplicationService;
        this.customerService = customerService;
    }

    @GetMapping
    public String listApplications(@RequestParam(value = "status", required = false) String status,
                                   @RequestParam(value = "product", required = false) String product,
                                   Model model) {
        List<LoanApplication> applications = loanApplicationService.getAllApplications();

        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            try {
                ApplicationStatus st = ApplicationStatus.valueOf(status.trim().toUpperCase());
                applications = applications.stream().filter(a -> a.getStatus() == st).toList();
            } catch (Exception ignored) {
            }
        }

        if (product != null && !product.isBlank() && !"ALL".equalsIgnoreCase(product)) {
            try {
                ProductType pt = ProductType.valueOf(product.trim().toUpperCase());
                applications = applications.stream().filter(a -> a.getProductType() == pt).toList();
            } catch (Exception ignored) {
            }
        }

        model.addAttribute("applications", applications);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedProduct", product);
        return "applications/list";
    }

    @GetMapping("/new")
    public String showNewApplicationForm(@RequestParam(value = "customerId", required = false) Long customerId,
                                         Model model) {
        LoanApplication app = new LoanApplication();
        if (customerId != null) {
            app.setCustomerId(customerId);
            try {
                model.addAttribute("selectedCustomer", customerService.getCustomerById(customerId));
            } catch (Exception ignored) {
            }
        }

        model.addAttribute("application", app);
        model.addAttribute("customers", customerService.getAllCustomers());
        return "applications/create";
    }

    @PostMapping("/draft")
    public String createDraftApplication(@ModelAttribute("application") LoanApplication application,
                                         RedirectAttributes redirectAttributes) {
        try {
            LoanApplication saved = loanApplicationService.createDraft(application);
            redirectAttributes.addFlashAttribute("successMessage",
                    "DRAFT loan application created with Number: " + saved.getApplicationNumber());
            return "redirect:/applications/" + saved.getApplicationId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/applications/new" + (application.getCustomerId() != null ? "?customerId=" + application.getCustomerId() : "");
        }
    }

    @GetMapping("/{id}")
    public String viewApplicationDetails(@PathVariable("id") Long id, Model model) {
        LoanApplication application = loanApplicationService.getApplicationById(id);
        List<ApplicationHistory> history = loanApplicationService.getApplicationHistory(id);

        model.addAttribute("application", application);
        model.addAttribute("historyList", history);
        return "applications/view";
    }

    @PostMapping("/{id}/submit")
    public String submitApplication(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            ApplicationSubmissionResponse response = loanApplicationService.submitApplication(id);
            if (response.isEligible()) {
                redirectAttributes.addFlashAttribute("successMessage",
                        "Application submitted and passed automated eligibility! Moved to UNDER_REVIEW.");
            } else {
                redirectAttributes.addFlashAttribute("warningMessage",
                        "Application was evaluated and REJECTED due to eligibility rules: " +
                                String.join("; ", response.getFailedReasons()));
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/applications/" + id;
    }

    @PostMapping("/{id}/edit")
    public String updateDraftApplication(@PathVariable("id") Long id,
                                         @ModelAttribute("application") LoanApplication application,
                                         RedirectAttributes redirectAttributes) {
        try {
            application.setApplicationId(id);
            loanApplicationService.updateDraft(application);
            redirectAttributes.addFlashAttribute("successMessage", "Draft application updated successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/applications/" + id;
    }
}
