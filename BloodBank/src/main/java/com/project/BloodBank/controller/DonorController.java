package com.project.BloodBank.controller;

import com.project.BloodBank.dto.UserProfileDto;
import com.project.BloodBank.model.enums.BloodGroup;
import com.project.BloodBank.model.enums.Gender;
import com.project.BloodBank.model.User;
import com.project.BloodBank.service.DonationService;
import com.project.BloodBank.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Set;

@Controller
@RequestMapping("/donor")
public class DonorController {

    private final UserService userService;
    private final DonationService donationService;

    public DonorController(UserService userService, DonationService donationService) {
        this.userService = userService;
        this.donationService = donationService;
    }

    @ModelAttribute("bloodGroups")
    public BloodGroup[] bloodGroups() {
        return BloodGroup.values();
    }

    @ModelAttribute("genders")
    public Gender[] genders() {
        return Gender.values();
    }

    // The profile form is shared by both flows, so the view has to be told where to post back to.
    private static final String PROFILE_EDIT_ACTION = "/donor/profile-edit";
    private static final String COMPLETE_PROFILE_ACTION = "/donor/complete-profile";

    private static final Set<String> HISTORY_SORT_FIELDS =
            Set.of("donationDate", "location", "unitsDonated");

    private static final Set<String> SEARCH_SORT_FIELDS =
            Set.of("fullName", "bloodGroup", "phoneNumber", "lastDonationDate", "address");

    @GetMapping("/profile")
    public String viewProfile(
            @RequestParam(defaultValue = "donationDate") String sort,
            @RequestParam(defaultValue = "desc") String dir,
            Model model) {

        String property = HISTORY_SORT_FIELDS.contains(sort) ? sort : "donationDate";
        Sort.Direction direction = "asc".equalsIgnoreCase(dir) ? Sort.Direction.ASC : Sort.Direction.DESC;

        User currentUser = userService.getCurrentUser();
        model.addAttribute("user", currentUser);
        model.addAttribute("donations",
                donationService.getDonationHistory(currentUser, Sort.by(direction, property)));
        model.addAttribute("sort", property);
        model.addAttribute("dir", direction.isAscending() ? "asc" : "desc");
        return "donor/profile";
    }

    @GetMapping("/profile-edit")
    public String editProfileForm(Model model) {
        User currentUser = userService.getCurrentUser();
        model.addAttribute("formAction", PROFILE_EDIT_ACTION);
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
            Model model,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            model.addAttribute("formAction", PROFILE_EDIT_ACTION);
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
        String view = editProfileForm(model);
        // Overrides what editProfileForm set, so the shared form posts back here instead.
        model.addAttribute("formAction", COMPLETE_PROFILE_ACTION);
        return view;
    }

    @PostMapping("/complete-profile")
    public String completeProfile(
            @Valid @ModelAttribute("profileDto") UserProfileDto dto,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            model.addAttribute("formAction", COMPLETE_PROFILE_ACTION);
            return "donor/profile-edit";
        }

        try {
            User currentUser = userService.getCurrentUser();
            userService.updateProfile(currentUser.getId(), dto);
            redirectAttributes.addFlashAttribute("success", "Profile completed! You can now request donations.");
            return "redirect:/donor/profile";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to complete profile: " + e.getMessage());
            return "redirect:/donor/complete-profile";
        }
    }

    @GetMapping("/search")
    public String searchDonors(
            @RequestParam(required = false) BloodGroup bloodGroup,
            @RequestParam(defaultValue = "fullName") String sort,
            @RequestParam(defaultValue = "asc") String dir,
            Model model) {

        String property = SEARCH_SORT_FIELDS.contains(sort) ? sort : "fullName";
        Sort.Direction direction = "desc".equalsIgnoreCase(dir) ? Sort.Direction.DESC : Sort.Direction.ASC;

        if (bloodGroup != null) {
            model.addAttribute("results",
                    userService.searchDonorsByBloodGroup(bloodGroup, Sort.by(direction, property)));
            model.addAttribute("selectedBloodGroup", bloodGroup);
        }

        model.addAttribute("sort", property);
        model.addAttribute("dir", direction.isAscending() ? "asc" : "desc");
        return "donor/search";
    }
}
