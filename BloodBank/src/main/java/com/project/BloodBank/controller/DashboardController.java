package com.project.BloodBank.controller;

import com.project.BloodBank.model.enums.BloodGroup;
import com.project.BloodBank.model.enums.RequestStatus;
import com.project.BloodBank.service.DashboardService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

@Controller
@RequestMapping("/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public String dashboard(Model model) {
        // Basic stats
        long totalDonors = dashboardService.getTotalActiveDonors();
        long donationsThisMonth = dashboardService.getDonationsThisMonth();
        long pendingRequests = dashboardService.getPendingRequestCount();

        // Grouped stats
        Map<BloodGroup, Long> donationsByBloodGroup = dashboardService.getDonationsByBloodGroup();
        Map<RequestStatus, Long> requestStatusCounts = dashboardService.getRequestStatusCounts();

        model.addAttribute("totalDonors", totalDonors);
        model.addAttribute("donationsThisMonth", donationsThisMonth);
        model.addAttribute("pendingRequests", pendingRequests);
        model.addAttribute("donationsByBloodGroup", donationsByBloodGroup);
        model.addAttribute("requestStatusCounts", requestStatusCounts);
        model.addAttribute("bloodGroups", BloodGroup.values());
        model.addAttribute("requestStatuses", RequestStatus.values());

        return "dashboard";
    }
}