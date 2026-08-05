package com.project.BloodBank.controller;

import com.project.BloodBank.dto.DonationRequestDto;
import com.project.BloodBank.model.DonationRequest;
import com.project.BloodBank.model.User;
import com.project.BloodBank.model.enums.BloodGroup;
import com.project.BloodBank.model.enums.UrgencyLevel;
import com.project.BloodBank.service.DonationRequestService;
import com.project.BloodBank.service.DonationService;
import com.project.BloodBank.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

// A donor's own requests: raising one, listing them, and viewing one in detail.
//
// The admin side of the same data lives in AdminController. The detail page below is shared by
// both, which is why it has to work out where "Back" should go.
@Controller
@RequestMapping("/requests")
public class DonationRequestController {

    private final DonationRequestService requestService;
    private final UserService userService;
    private final DonationService donationService;
    private final PageSupport pageSupport;

    public DonationRequestController(DonationRequestService requestService, UserService userService,
                                     DonationService donationService, PageSupport pageSupport) {
        this.requestService = requestService;
        this.userService = userService;
        this.donationService = donationService;
        this.pageSupport = pageSupport;
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
            // The requester is taken from the session, never from the form. Otherwise anyone could
            // raise a request in somebody else's name by editing the posted data.
            User currentUser = userService.getCurrentUser();
            DonationRequest request = requestService.createRequest(dto, currentUser);
            redirectAttributes.addFlashAttribute("success", "Donation request created successfully!");
            return "redirect:/requests/list";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to create request: " + e.getMessage());
            return "redirect:/requests/create";
        }
    }

    // As in AdminController: allowed ?sort= values mapped to what they really order on. This list
    // is shorter, because a donor's own list has no requester column to sort by.
    private static final Map<String, String> LIST_SORT_FIELDS = Map.of(
            "requestDate", "requestDate",
            "requestedBloodGroup", "requestedBloodGroup",
            "unitsNeeded", "unitsNeeded",
            "hospitalName", "hospitalName",
            "urgencyLevel", "urgencySeverity",
            "status", "status");

    @GetMapping("/list")
    public String listRequests(
            @RequestParam(defaultValue = "requestDate") String sort,
            @RequestParam(defaultValue = "desc") String dir,
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        SortRequest sorting = SortRequest.of(
                LIST_SORT_FIELDS, sort, dir, "requestDate", Sort.Direction.DESC);

        User currentUser = userService.getCurrentUser();
        Page<DonationRequest> requests = requestService.getRequestsByUser(currentUser,
                pageSupport.of(page, sorting.toSort()));
        model.addAttribute("requests", requests.getContent());
        model.addAttribute("page", requests);
        sorting.applyTo(model);
        return "requests/list";
    }

    // A record: a small immutable class where the fields, constructor, equals, hashCode and
    // toString are all generated. Ideal for a pair of values that only travel together.
    private record ReturnTarget(String url, String label) {
    }

    private static final ReturnTarget MY_REQUESTS =
            new ReturnTarget("/requests/list", "Back to my requests");

    // Where "Back" goes on the detail page, which depends on where you came from.
    //
    // The URL is looked up from a short token rather than read out of the query string. Accepting
    // ?from=/some/url and redirecting to it would be an open redirect: a link could send someone
    // from this trusted domain to an attacker's page. Anything not in this map falls back.
    private static final Map<String, ReturnTarget> RETURN_TARGETS = Map.of(
            "pending", new ReturnTarget("/admin/pending", "Back to pending queue"),
            "all", new ReturnTarget("/admin/requests", "Back to all requests"));

    @GetMapping("/{id}")
    public String viewRequest(
            @PathVariable Long id,
            @RequestParam(required = false) String from,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {
            User currentUser = userService.getCurrentUser();
            DonationRequest request = requestService.getRequestById(id);

            boolean isAdmin = currentUser.getRole().name().equals("ADMIN");

            // SecurityConfig can only say "signed in" for this path, because whether you may see a
            // particular request depends on whose it is. That check has to happen here, with the
            // row in hand. Without it, changing the number in the URL would show anyone's request.
            if (!request.getRequestedBy().getId().equals(currentUser.getId()) && !isAdmin) {
                redirectAttributes.addFlashAttribute("error", "You don't have permission to view this request.");
                return "redirect:/requests/list";
            }

            // The admin destinations are ignored for donors, who would only get a 403 there.
            ReturnTarget back = isAdmin && from != null
                    ? RETURN_TARGETS.getOrDefault(from, MY_REQUESTS)
                    : MY_REQUESTS;

            model.addAttribute("request", request);
            // A request can need more units than one donation provides, so the detail page shows
            // how far along it is rather than only what was asked for.
            model.addAttribute("unitsCollected", donationService.collectedFor(request.getId()));
            model.addAttribute("backUrl", back.url());
            model.addAttribute("backLabel", back.label());
            return "requests/detail";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Request not found.");
            return "redirect:/requests/list";
        }
    }
}