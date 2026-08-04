package com.project.BloodBank.controller;

import com.project.BloodBank.dto.UserProfileDto;
import com.project.BloodBank.exception.ResourceNotFoundException;
import com.project.BloodBank.model.Donation;
import com.project.BloodBank.model.DonationRequest;
import com.project.BloodBank.model.enums.BloodGroup;
import com.project.BloodBank.model.enums.Gender;
import com.project.BloodBank.model.enums.RequestStatus;
import com.project.BloodBank.model.enums.Role;
import com.project.BloodBank.model.User;
import com.project.BloodBank.service.DonationRequestService;
import com.project.BloodBank.service.DonationService;
import com.project.BloodBank.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    private final DonationRequestService requestService;
    private final PageSupport pageSupport;

    public DonorController(UserService userService, DonationService donationService,
                           DonationRequestService requestService, PageSupport pageSupport) {
        this.userService = userService;
        this.donationService = donationService;
        this.requestService = requestService;
        this.pageSupport = pageSupport;
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

    private static final Set<String> SEARCH_SORT_FIELDS_ADMIN =
            Set.of("fullName", "bloodGroup", "phoneNumber", "lastDonationDate", "address");

    /**
     * Non-administrators never see the phone or address columns, so they must not be able to order
     * by them either: sorting on a hidden column still leaks its relative ordering.
     */
    private static final Set<String> SEARCH_SORT_FIELDS_MEMBER =
            Set.of("fullName", "bloodGroup", "lastDonationDate");

    @GetMapping("/profile")
    public String viewProfile(
            @RequestParam(defaultValue = "donationDate") String sort,
            @RequestParam(defaultValue = "desc") String dir,
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        SortRequest sorting = SortRequest.of(
                HISTORY_SORT_FIELDS, sort, dir, "donationDate", Sort.Direction.DESC);

        User currentUser = userService.getCurrentUser();
        Page<Donation> donations = donationService.getDonationHistory(currentUser,
                pageSupport.of(page, sorting.toSort()));
        model.addAttribute("user", currentUser);
        model.addAttribute("donations", donations.getContent());
        model.addAttribute("page", donations);
        sorting.applyTo(model);
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
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) Long requestId,
            @AuthenticationPrincipal User principal,
            Model model) {

        boolean isAdmin = principal != null && principal.getRole() == Role.ADMIN;

        // Searching "on behalf of" an approved request. Admin-only, and ignored once the request
        // is no longer APPROVED so a stale link cannot reopen a finished job.
        if (isAdmin && requestId != null) {
            try {
                DonationRequest fulfilling = requestService.getRequestById(requestId);
                if (fulfilling.getStatus() == RequestStatus.APPROVED) {
                    model.addAttribute("fulfillingRequest", fulfilling);
                }
            } catch (ResourceNotFoundException e) {
                // Deleted since the link was made; fall through to an ordinary search.
            }
        }

        Set<String> allowed = isAdmin
                ? SEARCH_SORT_FIELDS_ADMIN
                : SEARCH_SORT_FIELDS_MEMBER;
        SortRequest sorting = SortRequest.of(allowed, sort, dir, "fullName", Sort.Direction.ASC);

        if (bloodGroup != null) {
            Page<User> results = userService.searchCompatibleDonors(bloodGroup,
                    pageSupport.of(page, sorting.toSort()));
            model.addAttribute("results", results.getContent());
            model.addAttribute("page", results);
            model.addAttribute("selectedBloodGroup", bloodGroup);
        }

        sorting.applyTo(model);
        return "donor/search";
    }
}
