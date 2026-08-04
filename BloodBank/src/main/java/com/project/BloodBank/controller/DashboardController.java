package com.project.BloodBank.controller;

import com.project.BloodBank.dto.ChartBar;
import com.project.BloodBank.model.enums.BloodGroup;
import com.project.BloodBank.model.enums.RequestStatus;
import com.project.BloodBank.service.DashboardService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.List;
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
        model.addAttribute("totalDonors", dashboardService.getTotalActiveDonors());
        model.addAttribute("donationsThisMonth", dashboardService.getDonationsThisMonth());
        model.addAttribute("pendingRequests", dashboardService.getPendingRequestCount());
        model.addAttribute("requestsRaisedToday", dashboardService.getRequestsRaisedToday());
        model.addAttribute("requestsDecidedToday", dashboardService.getRequestsDecidedToday());

        model.addAttribute("statusBars", statusBars());
        model.addAttribute("bloodGroupBars", bloodGroupBars());

        return "dashboard";
    }

    /** One bar per request status, coloured by status and split by what changed today. */
    private List<ChartBar> statusBars() {
        Map<RequestStatus, Long> totals = dashboardService.getRequestStatusCounts();
        Map<RequestStatus, Long> today = dashboardService.getRequestStatusCountsToday();

        List<ChartBar> bars = new ArrayList<>();
        for (RequestStatus status : RequestStatus.values()) {
            bars.add(ChartBar.of(
                    status.name(),
                    status.name().toLowerCase(),
                    totals.getOrDefault(status, 0L),
                    today.getOrDefault(status, 0L)));
        }
        return bars;
    }

    private List<ChartBar> bloodGroupBars() {
        Map<BloodGroup, Long> totals = dashboardService.getDonationsByBloodGroup();
        Map<BloodGroup, Long> today = dashboardService.getDonationsByBloodGroupToday();

        List<ChartBar> bars = new ArrayList<>();
        for (BloodGroup group : BloodGroup.values()) {
            bars.add(ChartBar.of(
                    group.getDisplayName(),
                    "blood",
                    totals.getOrDefault(group, 0L),
                    today.getOrDefault(group, 0L)));
        }
        return bars;
    }
}
