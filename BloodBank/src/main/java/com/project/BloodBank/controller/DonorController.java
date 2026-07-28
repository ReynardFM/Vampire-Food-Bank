package com.project.BloodBank.controller;

import com.project.BloodBank.dto.UserProfileDto;
import com.project.BloodBank.model.enums.BloodGroup;
import com.project.BloodBank.model.User;
import com.project.BloodBank.service.DonationService;
import com.project.BloodBank.service.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/donor")
public class DonorController {

    private final UserService userService;
    private final DonationService donationService;

    public DonorController(UserService userService, DonationService donationService) {
        this.userService = userService;
        this.donationService = donationService;
    }

    @GetMapping("/profile")
    public String viewProfile(Model model) {
        User currentUser = userService.getCurrentUser();
        model.addAttribute("user", currentUser);
        model.addAttribute("donations", donationService.getDonationHistory(currentUser));
        return "donor/profile";
    }

    @GetMapping("/profile-edit")
    public String editProfileForm(Model model) {
        User currentUser = userService.getCurrentUser();
        if (!model.containsAttribute("profileDto")) {
            UserProfileDto dto = new UserProfileDto();
            dto.setFullName(currentUser.getFullName());
            dto.setPhoneNumber(currentUser.getPhoneNumber());
            dto.setBloodGroup(currentUser.getBloodGroup());
            dto.setDateOfBirth(currentUser.getDateOfBirth());
            dto.setGender(currentUser.getGender());
            dto.setAddress(currentUser.getAddress());
            model.addAttribute("profileDto", dto);
        }
        return "donor/profile-edit";
    }

    @PostMapping("/profile-edit")
    public String updateProfile(
            @Valid @ModelAttribute("profileDto") UserProfileDto dto,
            BindingResult result,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            return "donor/profile-edit";
        }

        try {
            User currentUser = userService.getCurrentUser();
            userService.updateProfile(currentUser.getId(), dto);
            redirectAttributes.addFlashAttribute("success", "Profile updated successfully!");
            return "redirect:/donor/profile";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to update profile: " + e.getMessage());
            return "redirect:/donor/profile-edit";
        }
    }

    @GetMapping("/complete-profile")
    public String completeProfileForm(Model model) {
        return editProfileForm(model);
    }

    @PostMapping("/complete-profile")
    public String completeProfile(
            @Valid @ModelAttribute("profileDto") UserProfileDto dto,
            BindingResult result,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            return "donor/profile-edit";
        }

        try {
            User currentUser = userService.getCurrentUser();
            userService.updateProfile(currentUser.getId(), dto);
            redirectAttributes.addFlashAttribute("success", "Profile completed! You can now request donations.");
            return "redirect:/donor/profile";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to complete profile: " + e.getMessage());
            return "redirect:/register/complete-profile";
        }
    }

    @GetMapping("/search")
    public String searchDonors(
            @RequestParam(required = false) BloodGroup bloodGroup,
            Model model) {

        model.addAttribute("bloodGroups", BloodGroup.values());

        if (bloodGroup != null) {
            model.addAttribute("results", userService.searchDonorsByBloodGroup(bloodGroup));
            model.addAttribute("selectedBloodGroup", bloodGroup);
        }

        return "donor/search";
    }
}