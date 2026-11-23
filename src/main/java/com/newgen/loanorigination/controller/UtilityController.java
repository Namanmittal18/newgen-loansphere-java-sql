package com.newgen.loanorigination.controller;

import com.newgen.loanorigination.config.BusinessClock;
import com.newgen.loanorigination.dao.CoreBankingDAO;
import com.newgen.loanorigination.dao.SystemAlertDAO;
import com.newgen.loanorigination.dao.UtilityRunDAO;
import com.newgen.loanorigination.scheduler.CoreBankingScheduler;
import com.newgen.loanorigination.scheduler.OverdueReviewScheduler;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
@RequestMapping("/utilities")
public class UtilityController {

    private final OverdueReviewScheduler overdueReviewScheduler;
    private final CoreBankingScheduler coreBankingScheduler;
    private final UtilityRunDAO utilityRunDAO;
    private final SystemAlertDAO systemAlertDAO;
    private final CoreBankingDAO coreBankingDAO;
    private final BusinessClock businessClock;

    public UtilityController(OverdueReviewScheduler overdueReviewScheduler,
                             CoreBankingScheduler coreBankingScheduler,
                             UtilityRunDAO utilityRunDAO,
                             SystemAlertDAO systemAlertDAO,
                             CoreBankingDAO coreBankingDAO,
                             BusinessClock businessClock) {
        this.overdueReviewScheduler = overdueReviewScheduler;
        this.coreBankingScheduler = coreBankingScheduler;
        this.utilityRunDAO = utilityRunDAO;
        this.systemAlertDAO = systemAlertDAO;
        this.coreBankingDAO = coreBankingDAO;
        this.businessClock = businessClock;
    }

    @GetMapping
    public String showUtilitiesCenter(Model model) {
        model.addAttribute("utilityRuns", utilityRunDAO.findAll());
        model.addAttribute("alerts", systemAlertDAO.findAll());
        model.addAttribute("coreBankingRecords", coreBankingDAO.findAll());
        model.addAttribute("currentBusinessDate", businessClock.getCurrentDate());
        return "utilities/index";
    }

    @PostMapping("/overdue/run")
    public String runOverdueUtilityNow(@RequestParam(value = "simulatedDate", required = false)
                                       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate simulatedDate,
                                       RedirectAttributes redirectAttributes) {
        try {
            OverdueReviewScheduler.UtilityExecutionSummary summary =
                    overdueReviewScheduler.runUtilityNow(simulatedDate);

            redirectAttributes.addFlashAttribute("successMessage", String.format(
                    "Overdue Review Utility executed successfully! [Run #%d] Processed: %d, Alerts Created: %d, CSV Export: %s",
                    summary.getRunId(), summary.getProcessedCount(), summary.getAlertsCreated(), summary.getOutputFilePath()));
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Utility execution failed: " + e.getMessage());
        }
        return "redirect:/utilities";
    }

    @PostMapping("/corepush/run")
    public String runCoreDataPushNow(RedirectAttributes redirectAttributes) {
        try {
            CoreBankingScheduler.CorePushExecutionSummary summary = coreBankingScheduler.runCoreDataPushNow();
            redirectAttributes.addFlashAttribute("successMessage", String.format(
                    "CoreDataPush Worker finished. Records Claimed: %d, Successfully Synced to NG_CORE_BANKING: %d",
                    summary.getProcessedCount(), summary.getSuccessfulCount()));
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "CoreDataPush execution failed: " + e.getMessage());
        }
        return "redirect:/utilities";
    }
}
