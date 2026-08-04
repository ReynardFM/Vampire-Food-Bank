package com.project.BloodBank.controller;

import com.project.BloodBank.dto.DonationRequestDto;
import com.project.BloodBank.model.DonationRequest;
import com.project.BloodBank.model.User;
import com.project.BloodBank.model.enums.BloodGroup;
import com.project.BloodBank.model.enums.UrgencyLevel;
import com.project.BloodBank.service.DonationRequestService;
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

    /**
     * URL-facing sort name to the property actually ordered on. Urgency differs because the enum
     * column sorts alphabetically rather than by severity.
     */
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

        String requested = LIST_SORT_FIELDS.containsKey(sort) ? sort : "requestDate";
        Sort.Direction direction = "asc".equalsIgnoreCase(dir) ? Sort.Direction.ASC : Sort.Direction.DESC;

        User currentUser = userService.getCurrentUser();
        Page<DonationRequest> requests = requestService.getRequestsByUser(currentUser,
                PageSupport.of(page, Sort.by(direction, LIST_SORT_FIELDS.get(requested))));
        model.addAttribute("requests", requests.getContent());
        model.addAttribute("page", requests);
        model.addAttribute("sort", requested);
        model.addAttribute("dir", direction.isAscending() ? "asc" : "desc");
        return "requests/list";
    }

    private record ReturnTarget(String url, String label) {
    }

    private static final ReturnTarget MY_REQUESTS =
            new ReturnTarget("/requests/list", "Back to my requests");

    /**
     * Where "Back" goes on the detail page. Keyed by a short token rather than taking a URL from
     * the query string, so this cannot be turned into an open redirect.
     */
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

            // Security check: only owner or admin can view
            if (!request.getRequestedBy().getId().equals(currentUser.getId()) && !isAdmin) {
                redirectAttributes.addFlashAttribute("error", "You don't have permission to view this request.");
                return "redirect:/requests/list";
            }

            // The admin targets are ignored for donors, who would only get a 403 there.
            ReturnTarget back = isAdmin && from != null
                    ? RETURN_TARGETS.getOrDefault(from, MY_REQUESTS)
                    : MY_REQUESTS;

            model.addAttribute("request", request);
            model.addAttribute("backUrl", back.url());
            model.addAttribute("backLabel", back.label());
            return "requests/detail";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Request not found.");
            return "redirect:/requests/list";
        }
    }
}