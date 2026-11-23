package com.newgen.loanorigination.controller;

import com.newgen.loanorigination.model.Customer;
import com.newgen.loanorigination.model.LoanApplication;
import com.newgen.loanorigination.service.CustomerService;
import com.newgen.loanorigination.service.LoanApplicationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/customers")
public class CustomerController {

    private final CustomerService customerService;
    private final LoanApplicationService loanApplicationService;

    public CustomerController(CustomerService customerService, LoanApplicationService loanApplicationService) {
        this.customerService = customerService;
        this.loanApplicationService = loanApplicationService;
    }

    @GetMapping
    public String listCustomers(@RequestParam(value = "search", required = false) String search, Model model) {
        List<Customer> customers = customerService.searchCustomers(search);
        model.addAttribute("customers", customers);
        model.addAttribute("searchTerm", search);
        return "customers/list";
    }

    @GetMapping("/new")
    public String showRegistrationForm(Model model) {
        model.addAttribute("customer", new Customer());
        return "customers/create";
    }

    @PostMapping
    public String registerCustomer(@ModelAttribute("customer") Customer customer,
                                   RedirectAttributes redirectAttributes) {
        try {
            Customer saved = customerService.registerCustomer(customer);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Customer " + saved.getFullName() + " registered successfully with Code: " + saved.getCustomerCode());
            return "redirect:/customers/" + saved.getCustomerId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/customers/new";
        }
    }

    @GetMapping("/{id}")
    public String viewCustomer(@PathVariable("id") Long id, Model model) {
        Customer customer = customerService.getCustomerById(id);
        List<LoanApplication> applications = loanApplicationService.getApplicationsByCustomerId(id);

        model.addAttribute("customer", customer);
        model.addAttribute("applications", applications);
        return "customers/view";
    }

    @PostMapping("/{id}/edit")
    public String updateCustomer(@PathVariable("id") Long id,
                                 @ModelAttribute("customer") Customer customer,
                                 RedirectAttributes redirectAttributes) {
        try {
            customer.setCustomerId(id);
            customerService.updateCustomer(customer);
            redirectAttributes.addFlashAttribute("successMessage", "Customer updated successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/customers/" + id;
    }
}
