package com.project.BloodBank.controller;

import com.project.BloodBank.model.DonationRequest;
import com.project.BloodBank.model.User;
import com.project.BloodBank.service.DonationRequestService;
import com.project.BloodBank.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final DonationRequestService requestService;
    private final UserService userService;

    public AdminController(DonationRequestService requestService, UserService userService) {
        this.requestService = requestService;
        this.userService = userService;
    }

    @GetMapping("/pending")
    public String viewPendingRequests(Model model, RedirectAttributes redirectAttributes) {
        try {
            List<DonationRequest> pendingRequests = requestService.getPendingRequests();
            model.addAttribute("requests", pendingRequests);
            return "admin/pending-requests";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to load pending requests.");
            return "redirect:/dashboard";
        }
    }

    @PostMapping("/approve/{id}")
    public String approveRequest(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        try {
            requestService.approveRequest(id);
            redirectAttributes.addFlashAttribute("success", "Request approved successfully!");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", "Request is no longer pending.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to approve request: " + e.getMessage());
        }
        return "redirect:/admin/pending";
    }

    @PostMapping("/reject/{id}")
    public String rejectRequest(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        try {
            requestService.rejectRequest(id);
            redirectAttributes.addFlashAttribute("success", "Request rejected.");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", "Request is no longer pending.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to reject request: " + e.getMessage());
        }
        return "redirect:/admin/pending";
    }

    @GetMapping("/donors")
    public String listDonors(Model model) {
        List<User> donors = userService.getAllActiveDonors();
        model.addAttribute("donors", donors);
        return "admin/donor-list";
    }

    @PostMapping("/deactivate/{id}")
    public String deactivateDonor(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        try {
            userService.deactivateUser(id);
            redirectAttributes.addFlashAttribute("success", "Donor deactivated successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to deactivate donor: " + e.getMessage());
        }
        return "redirect:/admin/donors";
    }
}