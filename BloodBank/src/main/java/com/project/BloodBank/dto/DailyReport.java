package com.project.BloodBank.dto;

import java.time.LocalDate;

// One day's activity, summarised: what came in, what was decided, and what was collected.
//
// A plain class with getters rather than a record, for the same reason as ChartBar - Thymeleaf's
// ${report.date} looks for getDate(), and a record generates date() instead.
public class DailyReport {

    private final LocalDate date;
    private final long requestsRaised;
    private final long requestsDecided;
    private final long donations;
    private final long unitsCollected;

    public DailyReport(LocalDate date, long requestsRaised, long requestsDecided,
                       long donations, long unitsCollected) {
        this.date = date;
        this.requestsRaised = requestsRaised;
        this.requestsDecided = requestsDecided;
        this.donations = donations;
        this.unitsCollected = unitsCollected;
    }

    public LocalDate getDate() {
        return date;
    }

    public long getRequestsRaised() {
        return requestsRaised;
    }

    // Approved, rejected or fulfilled on this day - not raised on it. A request raised three weeks
    // ago but approved today counts as today's work, which is the whole point of decidedAt.
    public long getRequestsDecided() {
        return requestsDecided;
    }

    public long getDonations() {
        return donations;
    }

    public long getUnitsCollected() {
        return unitsCollected;
    }

    // Days where nothing happened are left out of the report list entirely, rather than filling it
    // with rows of zeros.
    public boolean isEmpty() {
        return requestsRaised == 0 && requestsDecided == 0 && donations == 0;
    }
}
