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

/**
 * Recording a donation is what ties the two halves of the app together: it fills in the donor's
 * history, feeds the dashboard totals and the donor's lastDonationDate, and closes out the
 * approved request it is linked to.
 *
 * Donations are entered by an administrator on the donor's behalf, which is why this sits behind
 * the admin rules rather than under /donor.
 */
@Controller
@RequestMapping("/donations")
public class DonationController {

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
    public String recordForm(@PathVariable Long donorId, Model model,
                             RedirectAttributes redirectAttributes) {
        try {
            addFormContext(donorId, model);
        } catch (ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("error", "That donor is no longer active.");
            return "redirect:/admin/donors";
        }

        if (!model.containsAttribute("donationDto")) {
            DonationRecordDto dto = new DonationRecordDto();
            dto.setDonationDate(LocalDate.now());
            dto.setUnitsDonated(1);
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
            // recordDonation refuses to link a donation to anything that is not APPROVED.
            addFormContext(donorId, model);
            model.addAttribute("error", "That request is no longer approved, so it cannot be linked.");
            return "donations/record";
        } catch (Exception e) {
            log.error("Failed to record donation for donor {}", donorId, e);
            addFormContext(donorId, model);
            model.addAttribute("error", "Failed to record the donation. Please try again.");
            return "donations/record";
        }
    }

    private void addFormContext(Long donorId, Model model) {
        model.addAttribute("donor", userService.getUserById(donorId));
        model.addAttribute("approvedRequests", requestService.getApprovedRequests());
    }
}
