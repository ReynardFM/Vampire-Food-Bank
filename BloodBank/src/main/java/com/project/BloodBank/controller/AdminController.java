package com.project.BloodBank.controller;

import com.project.BloodBank.model.DonationRequest;
import com.project.BloodBank.model.User;
import com.project.BloodBank.model.enums.BloodGroup;
import com.project.BloodBank.service.DonationRequestService;
import com.project.BloodBank.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final DonationRequestService requestService;
    private final UserService userService;
    private final PageSupport pageSupport;

    public AdminController(DonationRequestService requestService, UserService userService,
                           PageSupport pageSupport) {
        this.requestService = requestService;
        this.userService = userService;
        this.pageSupport = pageSupport;
    }

    /**
     * Same purpose as DONOR_SORT_FIELDS: keep an unknown ?sort= from becoming a 500. Maps the name
     * used in the URL to the property actually sorted on, which differ for urgency because the
     * enum column sorts alphabetically rather than by severity.
     */
    private static final Map<String, String> REQUEST_SORT_FIELDS = Map.of(
            "requestDate", "requestDate",
            "requestedBy.fullName", "requestedBy.fullName",
            "requestedBloodGroup", "requestedBloodGroup",
            "unitsNeeded", "unitsNeeded",
            "hospitalName", "hospitalName",
            "urgencyLevel", "urgencySeverity",
            "status", "status");

    @GetMapping("/pending")
    public String viewPendingRequests(
            @RequestParam(defaultValue = "requestDate") String sort,
            @RequestParam(defaultValue = "desc") String dir,
            @RequestParam(defaultValue = "0") int page,
            Model model,
            RedirectAttributes redirectAttributes) {

        SortRequest sorting = SortRequest.of(
                REQUEST_SORT_FIELDS, sort, dir, "requestDate", Sort.Direction.DESC);

        try {
            Page<DonationRequest> pendingRequests = requestService.getPendingRequests(
                    pageSupport.of(page, sorting.toSort()));
            model.addAttribute("requests", pendingRequests.getContent());
            model.addAttribute("page", pendingRequests);
            // Lets the view flag rows that arrived today without doing date maths in Thymeleaf.
            model.addAttribute("today", LocalDate.now());
            sorting.applyTo(model);
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
            DonationRequest approved = requestService.approveRequest(id);
            BloodGroup needed = approved.getRequestedBloodGroup();

            // Approving is almost always followed by "so who can actually give?", so go straight
            // there with the group already filled in instead of sending the admin back to the
            // queue to navigate across by hand.
            redirectAttributes.addFlashAttribute("success",
                    "Request approved. Showing donors who can give to " + needed.getDisplayName() + ".");
            redirectAttributes.addAttribute("bloodGroup", needed);
            // Carried so the search page knows which request is being fulfilled, and can hand it
            // on to the donation form. The request staying APPROVED is what makes this resumable.
            redirectAttributes.addAttribute("requestId", approved.getId());
            return "redirect:/donor/search";
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

    /**
     * Only these may reach Sort. Anything else would raise PropertyReferenceException and turn a
     * hand-edited query string into a 500.
     */
    private static final Set<String> DONOR_SORT_FIELDS =
            Set.of("fullName", "email", "bloodGroup", "phoneNumber", "lastDonationDate", "role");

    /**
     * The pending queue filters to PENDING, so this is the only place a decided request stays
     * visible after it has been approved or rejected.
     */
    @GetMapping("/requests")
    public String listAllRequests(
            @RequestParam(defaultValue = "requestDate") String sort,
            @RequestParam(defaultValue = "desc") String dir,
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        SortRequest sorting = SortRequest.of(
                REQUEST_SORT_FIELDS, sort, dir, "requestDate", Sort.Direction.DESC);

        Page<DonationRequest> requests = requestService.getAllRequests(
                pageSupport.of(page, sorting.toSort()));
        model.addAttribute("requests", requests.getContent());
        model.addAttribute("page", requests);
        sorting.applyTo(model);
        return "admin/request-list";
    }

    @GetMapping("/donors")
    public String listDonors(
            @RequestParam(defaultValue = "fullName") String sort,
            @RequestParam(defaultValue = "asc") String dir,
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        SortRequest sorting = SortRequest.of(
                DONOR_SORT_FIELDS, sort, dir, "fullName", Sort.Direction.ASC);

        Page<User> donors = userService.getAllActiveUsers(
                pageSupport.of(page, sorting.toSort()));
        model.addAttribute("donors", donors.getContent());
        model.addAttribute("page", donors);
        sorting.applyTo(model);
        return "admin/donor-list";
    }

    @PostMapping("/deactivate/{id}")
    public String deactivateDonor(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        // Deactivating yourself blocks your own sign-in, and nothing in the app can create or
        // re-enable an administrator, so it would end admin access permanently.
        if (id.equals(userService.getCurrentUser().getId())) {
            redirectAttributes.addFlashAttribute("error", "You cannot deactivate your own account.");
            return "redirect:/admin/donors";
        }

        try {
            userService.deactivateUser(id);
            redirectAttributes.addFlashAttribute("success", "Donor deactivated successfully.");
        } catch (IllegalStateException e) {
            // Raised when the target is an administrator.
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to deactivate donor: " + e.getMessage());
        }
        return "redirect:/admin/donors";
    }
}