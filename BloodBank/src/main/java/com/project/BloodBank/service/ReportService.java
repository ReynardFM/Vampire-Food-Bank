package com.project.BloodBank.service;

import com.project.BloodBank.dto.DailyReport;
import com.project.BloodBank.model.Donation;
import com.project.BloodBank.model.DonationRequest;
import com.project.BloodBank.repository.DonationRepository;
import com.project.BloodBank.repository.DonationRequestRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

// Builds the daily activity reports.
//
// The grouping by day is done in Java rather than with a GROUP BY, which is worth explaining. SQL
// can group by date perfectly well, but doing so needs a date-truncation function, and those differ
// between MySQL and the H2 database the tests run on. Fetching a window of rows and grouping them
// here behaves identically on both, and the volume is small: a month of a blood bank's activity is
// tens of rows, not thousands.
//
// If this ever had to cover years rather than a month, the grouping would belong in the database.
@Service
public class ReportService {

    // How far back the report list looks. A month is enough to see a pattern without the list
    // needing paging of its own.
    private static final int DAYS_COVERED = 30;

    private final DonationRequestRepository requestRepository;
    private final DonationRepository donationRepository;

    public ReportService(DonationRequestRepository requestRepository,
                         DonationRepository donationRepository) {
        this.requestRepository = requestRepository;
        this.donationRepository = donationRepository;
    }

    // One summary per day that had any activity, newest first. Quiet days are left out rather than
    // padding the list with rows of zeros.
    @Transactional(readOnly = true)
    public List<DailyReport> recentDays() {
        LocalDate from = LocalDate.now().minusDays(DAYS_COVERED - 1L);
        LocalDate to = LocalDate.now();

        // Counters keyed by day. TreeMap keeps the days in order, so reversing at the end gives
        // newest first without a separate sort.
        Map<LocalDate, long[]> tally = new TreeMap<>();

        // Index 0 raised, 1 decided, 2 donations, 3 units. An array rather than four maps, because
        // the alternative is walking the same days four times.
        for (DonationRequest request : requestsRaisedBetween(from, to)) {
            counters(tally, request.getRequestDate().toLocalDate())[0]++;
        }
        for (DonationRequest request : requestsDecidedBetween(from, to)) {
            counters(tally, request.getDecidedAt().toLocalDate())[1]++;
        }
        for (Donation donation : donationsBetween(from, to)) {
            long[] counts = counters(tally, donation.getDonationDate());
            counts[2]++;
            counts[3] += donation.getUnitsDonated();
        }

        List<DailyReport> reports = new ArrayList<>();
        for (Map.Entry<LocalDate, long[]> day : tally.entrySet()) {
            long[] c = day.getValue();
            reports.add(new DailyReport(day.getKey(), c[0], c[1], c[2], c[3]));
        }

        // Built oldest first by the TreeMap, shown newest first.
        java.util.Collections.reverse(reports);
        return reports;
    }

    // The same summary for a single day, used by the detail page. Recomputed rather than looked up
    // in the list above, so a day outside the 30-day window still opens if somebody types the date.
    @Transactional(readOnly = true)
    public DailyReport summaryFor(LocalDate date) {
        List<Donation> donations = donationsOn(date);

        long units = 0;
        for (Donation donation : donations) {
            units += donation.getUnitsDonated();
        }

        return new DailyReport(date,
                requestsRaisedOn(date).size(),
                requestsDecidedOn(date).size(),
                donations.size(),
                units);
    }

    // --- The rows behind one day's figures ---

    @Transactional(readOnly = true)
    public List<DonationRequest> requestsRaisedOn(LocalDate date) {
        return requestsRaisedBetween(date, date);
    }

    @Transactional(readOnly = true)
    public List<DonationRequest> requestsDecidedOn(LocalDate date) {
        return requestsDecidedBetween(date, date);
    }

    @Transactional(readOnly = true)
    public List<Donation> donationsOn(LocalDate date) {
        return donationsBetween(date, date);
    }

    // --- Window helpers ---

    private List<DonationRequest> requestsRaisedBetween(LocalDate from, LocalDate to) {
        return requestRepository.findByRequestDateBetween(
                startOf(from), endOf(to), Sort.by(Sort.Direction.DESC, "requestDate"));
    }

    private List<DonationRequest> requestsDecidedBetween(LocalDate from, LocalDate to) {
        return requestRepository.findByDecidedAtBetween(
                startOf(from), endOf(to), Sort.by(Sort.Direction.DESC, "decidedAt"));
    }

    private List<Donation> donationsBetween(LocalDate from, LocalDate to) {
        return donationRepository.findByDonationDateBetween(
                from, to, Sort.by(Sort.Direction.DESC, "donationDate"));
    }

    // requestDate and decidedAt carry a time, so a whole day has to be expressed as the span from
    // midnight to the last instant before the next midnight. Comparing on the date alone would
    // silently drop everything after 00:00.
    private LocalDateTime startOf(LocalDate date) {
        return date.atStartOfDay();
    }

    private LocalDateTime endOf(LocalDate date) {
        return date.plusDays(1).atStartOfDay().minusNanos(1);
    }

    private long[] counters(Map<LocalDate, long[]> tally, LocalDate date) {
        return tally.computeIfAbsent(date, d -> new long[4]);
    }
}
