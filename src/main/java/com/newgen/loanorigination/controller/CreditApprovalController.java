package com.newgen.loanorigination.controller;

import com.newgen.loanorigination.model.LoanApplication;
import com.newgen.loanorigination.model.enums.ApplicationStatus;
import com.newgen.loanorigination.service.LoanApplicationService;
import com.newgen.loanorigination.service.PdfGenerationService;
import com.newgen.loanorigination.service.ReviewerService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/credit")
public class CreditApprovalController {

    private final ReviewerService reviewerService;
    private final LoanApplicationService loanApplicationService;
    private final PdfGenerationService pdfGenerationService;

    public CreditApprovalController(ReviewerService reviewerService,
                                    LoanApplicationService loanApplicationService,
                                    PdfGenerationService pdfGenerationService) {
        this.reviewerService = reviewerService;
        this.loanApplicationService = loanApplicationService;
        this.pdfGenerationService = pdfGenerationService;
    }

    @GetMapping
    public String listApprovedApplications(Model model) {
        List<LoanApplication> approved = reviewerService.getCreditApprovalPending();
        model.addAttribute("approvedApplications", approved);
        return "credit/list";
    }

    @GetMapping("/{id}")
    public String viewCreditDetails(@PathVariable("id") Long id, Model model) {
        LoanApplication app = loanApplicationService.getApplicationById(id);
        model.addAttribute("application", app);
        model.addAttribute("historyList", loanApplicationService.getApplicationHistory(id));
        return "credit/view";
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadApplicationFormPdf(@PathVariable("id") Long id) {
        LoanApplication app = loanApplicationService.getApplicationById(id);
        byte[] pdfBytes = pdfGenerationService.generateApplicationFormPdf(app, app.getCustomer());

        String filename = String.format("ApplicationForm_%s.pdf", app.getApplicationNumber());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    @PostMapping("/{id}/reject")
    public String rejectApplication(@PathVariable("id") Long id,
                                    @RequestParam("reviewerCode") String reviewerCode,
                                    @RequestParam("rejectionReason") String rejectionReason,
                                    RedirectAttributes redirectAttributes) {
        try {
            reviewerService.processReviewDecision(id, reviewerCode, ApplicationStatus.REJECTED, rejectionReason);
            redirectAttributes.addFlashAttribute("successMessage", "Application rejected by credit authority.");
            return "redirect:/credit";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/credit/" + id;
        }
    }
}
