package com.project.BloodBank.controller;

import com.project.BloodBank.dto.DonationRequestDto;
import com.project.BloodBank.model.DonationRequest;
import com.project.BloodBank.model.User;
import com.project.BloodBank.model.enums.BloodGroup;
import com.project.BloodBank.model.enums.UrgencyLevel;
import com.project.BloodBank.service.DonationRequestService;
import com.project.BloodBank.service.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/requests")
public class DonationRequestController {

    private final DonationRequestService requestService;
    private final UserService userService;

    public DonationRequestController(DonationRequestService requestService, UserService userService) {
        this.requestService = requestService;
        this.userService = userService;
    }

    @GetMapping("/create")
    public String createRequestForm(Model model) {
        if (!model.containsAttribute("requestDto")) {
            model.addAttribute("requestDto", new DonationRequestDto());
        }
        model.addAttribute("bloodGroups", BloodGroup.values());
        model.addAttribute("urgencyLevels", UrgencyLevel.values());
        return "requests/create";
    }

    @PostMapping("/create")
    public String createRequest(
            @Valid @ModelAttribute("requestDto") DonationRequestDto dto,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            model.addAttribute("bloodGroups", BloodGroup.values());
            model.addAttribute("urgencyLevels", UrgencyLevel.values());
            return "requests/create";
        }

        try {
            User currentUser = userService.getCurrentUser();
            DonationRequest request = requestService.createRequest(dto, currentUser);
            redirectAttributes.addFlashAttribute("success", "Donation request created successfully!");
            return "redirect:/requests/list";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to create request: " + e.getMessage());
            return "redirect:/requests/create";
        }
    }

    @GetMapping("/list")
    public String listRequests(Model model) {
        User currentUser = userService.getCurrentUser();
        model.addAttribute("requests", requestService.getRequestsByUser(currentUser));
        return "requests/list";
    }

    @GetMapping("/{id}")
    public String viewRequest(
            @PathVariable Long id,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {
            User currentUser = userService.getCurrentUser();
            DonationRequest request = requestService.getRequestById(id);

            // Security check: only owner or admin can view
            if (!request.getRequestedBy().getId().equals(currentUser.getId()) &&
                    !currentUser.getRole().name().equals("ADMIN")) {
                redirectAttributes.addFlashAttribute("error", "You don't have permission to view this request.");
                return "redirect:/requests/list";
            }

            model.addAttribute("request", request);
            return "requests/detail";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Request not found.");
            return "redirect:/requests/list";
        }
    }
}