package com.project.BloodBank.controller;

import com.project.BloodBank.service.ReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDate;

// Daily activity reports: a list of days, and one day in full.
//
// Sits under /admin/reports, so SecurityConfig's existing /admin/** rule locks it to administrators
// without anything being added there.
@Controller
@RequestMapping("/admin/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    // Every day in the last month that had activity, newest first.
    @GetMapping
    public String listReports(Model model) {
        model.addAttribute("reports", reportService.recentDays());
        model.addAttribute("today", LocalDate.now());
        return "admin/report-list";
    }

    // One day in full: the summary again, plus the rows behind each figure.
    //
    // @DateTimeFormat is what lets /admin/reports/2026-08-04 bind to a LocalDate. Without it Spring
    // has no idea how to read the path segment and the request fails before this method runs.
    //
    // Any date works, not only ones on the list. A day outside the 30-day window, or one with
    // nothing on it, renders an honest empty report rather than a 404 - there is no such thing as a
    // date that does not exist.
    @GetMapping("/{date}")
    public String viewReport(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Model model) {

        model.addAttribute("report", reportService.summaryFor(date));
        model.addAttribute("requestsRaised", reportService.requestsRaisedOn(date));
        model.addAttribute("requestsDecided", reportService.requestsDecidedOn(date));
        model.addAttribute("donations", reportService.donationsOn(date));
        return "admin/report-detail";
    }
}
