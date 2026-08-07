package com.project.BloodBank.controller;

import com.project.BloodBank.dto.DonationRecordDto;
import com.project.BloodBank.exception.ResourceNotFoundException;
import com.project.BloodBank.model.DonationRequest;
import com.project.BloodBank.model.User;
import com.project.BloodBank.model.enums.RequestStatus;
import com.project.BloodBank.service.DonationRequestService;
import com.project.BloodBank.service.DonationService;
import com.project.BloodBank.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

// Recording a donation, which is what ties the two halves of the application together: it fills in
// the donor's history, feeds the dashboard totals and their last donation date, and closes out the
// approved request it is linked to.
//
// Donations are entered by an administrator on the donor's behalf, never by donors themselves,
// which is why SecurityConfig locks /donations/record/** to ROLE_ADMIN even though /donations/**
// is otherwise open to any signed-in account.
@Controller
@RequestMapping("/donations")
public class DonationController {

    // A logger rather than System.out: it can be filtered by level, carries the class name and
    // timestamp, and is the only one of the two that captures a stack trace.
    private static final Logger log = LoggerFactory.getLogger(DonationController.class);

    private final DonationService donationService;
    private final DonationRequestService requestService;
    private final UserService userService;

    public DonationController(DonationService donationService,
                              DonationRequestService requestService,
                              UserService userService) {
        this.donationService = donationService;
        this.requestService = requestService;
        this.userService = userService;
    }

    @GetMapping("/record/{donorId}")
    public String recordForm(@PathVariable Long donorId,
                             @RequestParam(required = false) Long requestId,
                             @RequestParam(required = false) String from,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        // Carried through so that a partial donation returns to a search that still knows where the
        // fulfilment started. The form posts it straight back on its action URL.
        model.addAttribute("from", from);
        try {
            addFormContext(donorId, model);
        } catch (ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("error", "That donor is no longer active.");
            return "redirect:/admin/donors";
        }

        if (!model.containsAttribute("donationDto")) {
            DonationRecordDto dto = new DonationRecordDto();

            // Sensible starting values, since the overwhelmingly common case is one unit collected
            // today. Both remain editable.
            dto.setDonationDate(LocalDate.now());
            dto.setUnitsDonated(1);

            // The last step of the workflow. Arriving from a donor search that was fulfilling a
            // request preselects it, so the administrator does not have to find it again in a
            // dropdown they were just looking at.
            dto.setLinkedRequestId(requestId);
            model.addAttribute("donationDto", dto);
        }

        // Two separate questions about the request this form arrived with.
        //
        // fulfillingRequest is "which request sent me here", and drives where Cancel goes - back to
        // the search that was filling it, rather than the account list nobody came from.
        //
        // lockRequest is "may this donor actually fulfil it", which is what decides whether the
        // dropdown is fixed. They differ in one case that matters: the donor is the person who
        // raised the request, so the dropdown does not offer it. Then the way back is still the
        // search, but the choice has to stay open.
        if (requestId != null) {
            try {
                model.addAttribute("fulfillingRequest", requestService.getRequestById(requestId));
            } catch (ResourceNotFoundException e) {
                // Removed since the link was made. Cancel falls back to the account list.
            }

            // A preselection only holds if the dropdown actually offers that request. When it does
            // not, the browser falls back to "Not linked" and the donation would be filed as an
            // unrelated walk-in without a word. Saying so is the difference between a refusal and a
            // silent wrong answer.
            boolean offered = offersRequest(model, requestId);
            model.addAttribute("lockRequest", offered);

            if (!offered) {
                model.addAttribute("error",
                        "This donor cannot fulfil request #" + requestId
                                + ", so it is not selected below. Recording here files an unlinked donation.");
            }
        }

        return "donations/record";
    }

    @PostMapping("/record/{donorId}")
    public String recordDonation(
            @PathVariable Long donorId,
            @Valid @ModelAttribute("donationDto") DonationRecordDto dto,
            @RequestParam(required = false) String from,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {

        // The donor is looked up before the form is validated, because everything after this point
        // needs them - including redisplaying the form, which shows their name and blood group.
        User donor;
        try {
            donor = userService.getUserById(donorId);
        } catch (ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("error", "That donor is no longer active.");
            return "redirect:/admin/donors";
        }

        if (result.hasErrors()) {
            addFormContext(donorId, model);
            model.addAttribute("from", from);
            return "donations/record";
        }

        try {
            // The donor gives the blood; the signed-in administrator is the one entering it.
            donationService.recordDonation(dto, donor, userService.getCurrentUser());
            return afterRecording(dto, donor, from, redirectAttributes);
        } catch (IllegalStateException e) {
            // Raised by recordDonation for a request that is no longer approved, or one this donor's
            // blood cannot serve. The message says which, so it is shown rather than replaced.
            addFormContext(donorId, model);
            model.addAttribute("error", e.getMessage());
            return "donations/record";
        } catch (Exception e) {
            // Anything unexpected. The details go to the log for whoever maintains this, while the
            // user gets a plain message - an exception's text can expose internals, and would not
            // help them anyway.
            log.error("Failed to record donation for donor {}", donorId, e);
            addFormContext(donorId, model);
            model.addAttribute("error", "Failed to record the donation. Please try again.");
            return "donations/record";
        }
    }

    // Where to go once a donation is saved, which depends on whether it finished the job.
    //
    // A donation that only partly fills its request leaves work outstanding, and landing on the
    // account list at that point abandons it - the administrator has to find their way back to
    // search and remember which request they were filling. Sending them straight to donor search
    // with the request still in hand keeps the session going, exactly as approving one does.
    //
    // The request is re-read rather than taken from the saved donation. recordDonation runs in its
    // own transaction, so by the time this executes the entity it returned is detached and its
    // status is whatever it was before fulfilment was considered.
    private String afterRecording(DonationRecordDto dto, User donor, String from,
                                  RedirectAttributes redirectAttributes) {

        if (dto.getLinkedRequestId() == null) {
            redirectAttributes.addFlashAttribute("success",
                    "Donation recorded for " + donor.getFullName() + ".");
            return "redirect:/admin/donors";
        }

        DonationRequest linked = requestService.getRequestById(dto.getLinkedRequestId());

        // Still approved means it was not enough. Carry on collecting.
        if (linked.getStatus() == RequestStatus.APPROVED) {
            int outstanding = linked.getUnitsNeeded() - donationService.collectedFor(linked.getId());

            redirectAttributes.addFlashAttribute("success",
                    "Recorded " + dto.getUnitsDonated() + " unit(s) from " + donor.getFullName()
                            + ". Request #" + linked.getId() + " still needs " + outstanding + ".");
            redirectAttributes.addAttribute("bloodGroup", linked.getRequestedBloodGroup());
            redirectAttributes.addAttribute("requestId", linked.getId());
            redirectAttributes.addAttribute("from", from);
            return "redirect:/donor/search";
        }

        redirectAttributes.addFlashAttribute("success",
                "Donation recorded for " + donor.getFullName()
                        + ". Request #" + linked.getId() + " is now fulfilled.");
        return "redirect:/admin/donors";
    }

    // Whether the dropdown actually contains the request the URL asked to preselect. Read back off
    // the model rather than re-queried, so it is exactly the list the page will render.
    @SuppressWarnings("unchecked")
    private boolean offersRequest(Model model, Long requestId) {
        Object offered = model.getAttribute("approvedRequests");
        if (!(offered instanceof List<?> requests)) {
            return false;
        }

        return ((List<DonationRequest>) requests).stream()
                .anyMatch(request -> requestId.equals(request.getId()));
    }

    // What the form needs besides the DTO itself. Extracted because every path that redisplays the
    // form has to repeat it - a returned view keeps nothing from the request that failed.
    private void addFormContext(Long donorId, Model model) {
        User donor = userService.getUserById(donorId);
        model.addAttribute("donor", donor);
        // Only the requests this donor could actually fulfil. Offering every approved request
        // invited linking an A+ donation to an O- patient, which recordDonation now refuses -
        // better not to present the choice at all than to reject it after the fact.
        model.addAttribute("approvedRequests", requestService.getApprovedRequestsFor(donor));
    }
}
