package com.newgen.loanorigination.controller;

import com.newgen.loanorigination.model.LoanApplication;
import com.newgen.loanorigination.model.enums.ApplicationStatus;
import com.newgen.loanorigination.service.LoanApplicationService;
import com.newgen.loanorigination.service.ReviewerService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/reviewer")
public class ReviewerController {

    private final ReviewerService reviewerService;
    private final LoanApplicationService loanApplicationService;

    public ReviewerController(ReviewerService reviewerService, LoanApplicationService loanApplicationService) {
        this.reviewerService = reviewerService;
        this.loanApplicationService = loanApplicationService;
    }

    @GetMapping
    public String listPendingReviews(Model model) {
        List<LoanApplication> pending = reviewerService.getPendingReviews();
        model.addAttribute("pendingApplications", pending);
        return "reviewer/list";
    }

    @GetMapping("/{id}")
    public String reviewApplication(@PathVariable("id") Long id, Model model) {
        LoanApplication app = loanApplicationService.getApplicationById(id);
        model.addAttribute("application", app);
        model.addAttribute("historyList", loanApplicationService.getApplicationHistory(id));
        return "reviewer/view";
    }

    @PostMapping("/{id}/decision")
    public String recordDecision(@PathVariable("id") Long id,
                                 @RequestParam("reviewerCode") String reviewerCode,
                                 @RequestParam("decision") String decision,
                                 @RequestParam(value = "remarks", required = false) String remarks,
                                 RedirectAttributes redirectAttributes) {
        try {
            ApplicationStatus target = ApplicationStatus.valueOf(decision.trim().toUpperCase());
            LoanApplication decided = reviewerService.processReviewDecision(id, reviewerCode, target, remarks);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Reviewer decision recorded successfully. Application moved to " + decided.getStatus());
            return "redirect:/reviewer";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/reviewer/" + id;
        }
    }
}
