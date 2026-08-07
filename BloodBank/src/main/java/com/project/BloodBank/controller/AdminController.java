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

// The administrator's side of the application: the pending queue, every request, and accounts.
//
// @RequestMapping("/admin") prefixes every path below, so @GetMapping("/pending") serves
// /admin/pending. That prefix is also what SecurityConfig locks down with a single rule, which is
// why none of these methods checks the role itself.
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

    // Which ?sort= values are allowed, and what each one really orders on. A map rather than a set
    // because urgencyLevel has to order on urgencySeverity - sorting the enum's text is
    // alphabetical, so LOW lands in the middle.
    private static final Map<String, String> REQUEST_SORT_FIELDS = Map.of(
            "requestDate", "requestDate",
            "requestedBy.fullName", "requestedBy.fullName",
            "requestedBloodGroup", "requestedBloodGroup",
            "unitsNeeded", "unitsNeeded",
            "hospitalName", "hospitalName",
            "urgencyLevel", "urgencySeverity",
            "status", "status");

    // The review queue: everything still waiting for a decision.
    //
    // @RequestParam with a defaultValue means these are optional - arriving at /admin/pending with
    // no query string gives newest first, page zero.
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

    // A POST rather than a GET because it changes something. A GET would let a link, a bookmark or
    // a page preloader approve a request by accident, and Spring Security's CSRF protection only
    // covers the methods that are meant to write.
    //
    // Redirecting afterwards rather than rendering is the post/redirect/get pattern: it means a
    // browser refresh re-runs the harmless GET rather than approving a second time.
    @PostMapping("/approve/{id}")
    public String approveRequest(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        try {
            // The signed-in administrator is recorded against the decision.
            DonationRequest approved = requestService.approveRequest(id, userService.getCurrentUser());
            BloodGroup needed = approved.getRequestedBloodGroup();

            // Approving is almost always followed by "so who can actually give?", so go straight
            // there with the group filled in rather than sending the administrator back to the
            // queue to navigate across by hand.
            //
            // Note the two kinds of attribute. A flash attribute survives one redirect and then
            // disappears; addAttribute appends to the query string, which is what puts
            // ?bloodGroup=...&requestId=... on the URL and makes the resulting page linkable.
            redirectAttributes.addFlashAttribute("success",
                    "Request approved. Showing donors who can give to " + needed.getDisplayName() + ".");
            redirectAttributes.addAttribute("bloodGroup", needed);

            // Carried so the search page knows which request is being fulfilled and can hand it on
            // to the donation form. The request staying APPROVED is what makes this resumable: the
            // administrator can walk away and pick it up from the request list later.
            redirectAttributes.addAttribute("requestId", approved.getId());
            return "redirect:/donor/search";
        } catch (IllegalStateException e) {
            // Thrown by approveRequest when the request is no longer PENDING - usually because
            // another administrator got there first, or the page was left open too long.
            redirectAttributes.addFlashAttribute("error", "Request is no longer pending.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to approve request: " + e.getMessage());
        }
        return "redirect:/admin/pending";
    }

    // Rejecting has no follow-on step, so unlike approving it just returns to the queue.
    @PostMapping("/reject/{id}")
    public String rejectRequest(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        try {
            requestService.rejectRequest(id, userService.getCurrentUser());
            redirectAttributes.addFlashAttribute("success", "Request rejected.");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", "Request is no longer pending.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to reject request: " + e.getMessage());
        }
        return "redirect:/admin/pending";
    }

    // A plain set here, unlike REQUEST_SORT_FIELDS, because every one of these sorts on the column
    // it is named after. Note what is absent: password is a field on User, and without this list
    // ?sort=password would happily order the table by hash.
    private static final Set<String> DONOR_SORT_FIELDS =
            Set.of("fullName", "email", "bloodGroup", "phoneNumber", "lastDonationDate", "role");

    // Every request whatever its status. The pending queue filters to PENDING, so this is the only
    // place an approved or rejected request stays visible.
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

    // Account management. Administrators appear here too - safe now that deactivateUser refuses
    // them outright, so an admin row can be shown without being a hazard.
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

        // Two guards, in two places, for two different mistakes.
        //
        // This one is about the person clicking: deactivating yourself blocks your own sign-in, and
        // since nothing can create or re-enable an administrator, that would end admin access
        // permanently. It lives here because it needs to know who is signed in.
        if (id.equals(userService.getCurrentUser().getId())) {
            redirectAttributes.addFlashAttribute("error", "You cannot deactivate your own account.");
            return "redirect:/admin/donors";
        }

        try {
            userService.deactivateUser(id);
            redirectAttributes.addFlashAttribute("success", "Donor deactivated successfully.");
        } catch (IllegalStateException e) {
            // The other guard, raised by the service when the target is any administrator - not
            // just yourself. It sits in the service so it holds for every caller, not only this
            // screen. Its message is already written for the user, so it is passed straight on.
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to deactivate donor: " + e.getMessage());
        }
        return "redirect:/admin/donors";
    }
}