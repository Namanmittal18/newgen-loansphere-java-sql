package com.newgen.loanorigination.controller;

import com.newgen.loanorigination.dto.DisbursementResponse;
import com.newgen.loanorigination.model.Disbursement;
import com.newgen.loanorigination.model.LoanApplication;
import com.newgen.loanorigination.service.DisbursementService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/disbursement")
public class DisbursementController {

    private final DisbursementService disbursementService;

    public DisbursementController(DisbursementService disbursementService) {
        this.disbursementService = disbursementService;
    }

    @GetMapping
    public String showDisbursementWorkspace(Model model) {
        List<LoanApplication> readyList = disbursementService.getApplicationsReadyForDisbursement();
        List<Disbursement> historyList = disbursementService.getAllDisbursements();

        model.addAttribute("readyApplications", readyList);
        model.addAttribute("disbursements", historyList);
        model.addAttribute("totalDisbursedAmount", disbursementService.getTotalDisbursedAmount());
        model.addAttribute("totalDisbursedCount", disbursementService.getTotalDisbursementCount());
        return "disbursement/list";
    }

    @PostMapping("/{id}/execute")
    public String executeDisbursement(@PathVariable("id") Long id,
                                      @RequestParam(value = "performedBy", required = false) String performedBy,
                                      RedirectAttributes redirectAttributes) {
        try {
            DisbursementResponse response = disbursementService.disburseLoan(id, performedBy);
            redirectAttributes.addFlashAttribute("disbursementResponse", response);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Disbursement executed successfully! Reference: " + response.getDisbursementReference());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/disbursement";
    }
}
