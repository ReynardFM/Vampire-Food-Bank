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

// A donor's own area: their profile, their donation history, and donor search.
//
// Everything here is available to any signed-in account, not just donors. Administrators use the
// same search screen, and see more of it - see the two sort whitelists below.
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

    // @ModelAttribute on a method rather than a parameter: these run before every handler in this
    // class and add their return value to the model, so the dropdowns are populated on all of them
    // without each one repeating it.
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

    // Two whitelists, because the search results table shows different columns depending on who is
    // looking. Administrators get phone and address; ordinary members do not.
    private static final Set<String> SEARCH_SORT_FIELDS_ADMIN =
            Set.of("fullName", "bloodGroup", "phoneNumber", "lastDonationDate", "address");

    // The narrower list is a privacy measure, not tidiness. Sorting by a hidden column still leaks
    // it: page through ?sort=phoneNumber and you learn everyone's relative phone numbers without
    // the column ever being displayed.
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

    // Fills a DTO from the stored user so the form comes back with the current values in it.
    //
    // Copying field by field rather than handing the entity to the form is what stops the form
    // being able to reach anything it should not - there is no email, role or active on the DTO.
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

    // The same form under a different name, for a donor who has never filled it in. Only the
    // wording and the destination differ, so this reuses the method above rather than repeating it.
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

    // Donor search, and the second half of the approve-then-fulfil workflow.
    //
    // Three things arrive on the query string: the group being searched for, the usual sort and
    // page, and optionally a requestId when this was reached by approving a request.
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

        // Searching on behalf of an approved request, which is what turns this page into a step in
        // a workflow rather than a lookup. Admin-only, and quietly ignored once the request is no
        // longer APPROVED, so an old link cannot reopen a job that is already finished.
        // Whoever raised the request being fulfilled. They are excluded from the results below,
        // because a request cannot be fulfilled by the person who raised it - listing them offers a
        // donation that the recording step would refuse, and worse, the recording form would drop
        // the request link without saying so and file the donation as an unrelated walk-in.
        Long requesterToExclude = null;

        if (isAdmin && requestId != null) {
            try {
                DonationRequest fulfilling = requestService.getRequestById(requestId);
                if (fulfilling.getStatus() == RequestStatus.APPROVED) {
                    model.addAttribute("fulfillingRequest", fulfilling);
                    // How much is still outstanding, so the banner says what is actually left to
                    // collect rather than repeating the original figure on every visit.
                    model.addAttribute("unitsOutstanding",
                            fulfilling.getUnitsNeeded() - donationService.collectedFor(fulfilling.getId()));
                    requesterToExclude = fulfilling.getRequestedBy().getId();
                }
            } catch (ResourceNotFoundException e) {
                // Deleted since the link was made. Falling through to an ordinary search is kinder
                // than a 404, since the search itself is still perfectly valid.
            }
        }

        Set<String> allowed = isAdmin
                ? SEARCH_SORT_FIELDS_ADMIN
                : SEARCH_SORT_FIELDS_MEMBER;
        SortRequest sorting = SortRequest.of(allowed, sort, dir, "fullName", Sort.Direction.ASC);

        // With no group chosen the page shows the whole roster rather than an empty table. Arriving
        // at a search screen that has nothing on it gives no sense of what is even here, and the
        // first thing anybody does is pick a group at random to find out.
        //
        // selectedBloodGroup is left unset in that case, which is what the view keys off to decide
        // between "everyone" and "everyone who can give to X".
        Page<User> results;
        if (bloodGroup != null) {
            // Compatible donors, not exact matches - see BloodGroup.compatibleDonors(). The
            // exclusion is null on an ordinary search, so everybody compatible is listed.
            results = userService.searchCompatibleDonors(
                    bloodGroup, requesterToExclude, pageSupport.of(page, sorting.toSort()));
            model.addAttribute("selectedBloodGroup", bloodGroup);
        } else {
            results = userService.getAllActiveUsers(pageSupport.of(page, sorting.toSort()));
        }

        model.addAttribute("results", results.getContent());
        model.addAttribute("page", results);

        sorting.applyTo(model);
        return "donor/search";
    }
}
