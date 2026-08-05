package com.project.BloodBank.controller;

import com.project.BloodBank.dto.DonationRecordDto;
import com.project.BloodBank.exception.ResourceNotFoundException;
import com.project.BloodBank.model.User;
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
                             Model model,
                             RedirectAttributes redirectAttributes) {
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

        return "donations/record";
    }

    @PostMapping("/record/{donorId}")
    public String recordDonation(
            @PathVariable Long donorId,
            @Valid @ModelAttribute("donationDto") DonationRecordDto dto,
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
            return "donations/record";
        }

        try {
            donationService.recordDonation(dto, donor);
            redirectAttributes.addFlashAttribute("success",
                    "Donation recorded for " + donor.getFullName() + ".");
            return "redirect:/admin/donors";
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
