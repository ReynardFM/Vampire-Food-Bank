package com.project.BloodBank.service;

import com.project.BloodBank.dto.DailyReport;
import com.project.BloodBank.model.Donation;
import com.project.BloodBank.model.DonationRequest;
import com.project.BloodBank.model.User;
import com.project.BloodBank.model.enums.BloodGroup;
import com.project.BloodBank.model.enums.RequestStatus;
import com.project.BloodBank.model.enums.Role;
import com.project.BloodBank.model.enums.UrgencyLevel;
import com.project.BloodBank.repository.DonationRepository;
import com.project.BloodBank.repository.DonationRequestRepository;
import com.project.BloodBank.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The daily reports count two different things - when a request arrived and when it was decided -
 * and getting those confused is the exact bug that made the dashboard report old requests as
 * today's work. These tests pin down that separation, and the day boundaries around it.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ReportServiceTest {

    @Autowired
    private ReportService reportService;

    @Autowired
    private DonationRequestRepository requestRepository;

    @Autowired
    private DonationRepository donationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User donor;

    @BeforeEach
    void setUp() {
        donor = new User();
        donor.setEmail("report-donor@test.local");
        donor.setFullName("Report Donor");
        donor.setPassword(passwordEncoder.encode("Password123!"));
        donor.setRole(Role.DONOR);
        donor.setActive(true);
        donor.setBloodGroup(BloodGroup.A_POSITIVE);
        donor = userRepository.save(donor);
    }

    /** requestDate is updatable = false, so it has to be set before the first save, as the seeder does. */
    private DonationRequest request(LocalDateTime raisedAt, LocalDateTime decidedAt, RequestStatus status) {
        DonationRequest request = new DonationRequest();
        request.setRequestedBy(donor);
        request.setRequestedBloodGroup(BloodGroup.A_POSITIVE);
        request.setUnitsNeeded(1);
        request.setHospitalName("Test Hospital");
        request.setUrgencyLevel(UrgencyLevel.HIGH);
        request.setStatus(status);
        request.setRequestDate(raisedAt);
        request.setDecidedAt(decidedAt);
        return requestRepository.save(request);
    }

    private Donation donation(LocalDate on, int units) {
        Donation donation = new Donation();
        donation.setDonor(donor);
        donation.setDonationDate(on);
        donation.setUnitsDonated(units);
        donation.setLocation("Test Hospital");
        return donationRepository.save(donation);
    }

    /**
     * The heart of it. A request raised on one day and approved on another is the ordinary case, and
     * each day's report must claim only its own half.
     */
    @Test
    void aRequestCountsAsRaisedOnOneDayAndDecidedOnAnother() {
        LocalDate raised = LocalDate.now().minusDays(5);
        LocalDate decided = LocalDate.now().minusDays(2);

        request(raised.atTime(9, 0), decided.atTime(14, 0), RequestStatus.APPROVED);

        assertThat(reportService.summaryFor(raised).getRequestsRaised()).isEqualTo(1);
        assertThat(reportService.summaryFor(raised).getRequestsDecided()).isZero();

        assertThat(reportService.summaryFor(decided).getRequestsRaised()).isZero();
        assertThat(reportService.summaryFor(decided).getRequestsDecided()).isEqualTo(1);
    }

    /**
     * A pending request has no decision time, and neither do rows that predate the decided_at
     * column. Neither should ever be counted as decided on any day.
     */
    @Test
    void aRequestWithNoDecisionTimeIsNeverCountedAsDecided() {
        LocalDate raised = LocalDate.now().minusDays(3);

        request(raised.atTime(9, 0), null, RequestStatus.PENDING);

        assertThat(reportService.summaryFor(raised).getRequestsRaised()).isEqualTo(1);
        assertThat(reportService.summaryFor(raised).getRequestsDecided()).isZero();
    }

    /**
     * The window for a day runs to the last instant before the next midnight. Comparing on the date
     * alone would silently drop everything after 00:00.
     */
    @Test
    void aDecisionLateAtNightBelongsToThatDayAndNotTheNext() {
        LocalDate day = LocalDate.now().minusDays(4);

        request(day.atTime(9, 0), day.atTime(23, 59, 59), RequestStatus.APPROVED);

        assertThat(reportService.summaryFor(day).getRequestsDecided()).isEqualTo(1);
        assertThat(reportService.summaryFor(day.plusDays(1)).getRequestsDecided()).isZero();
    }

    @Test
    void donationsAndTheirUnitsAreBothCounted() {
        LocalDate day = LocalDate.now().minusDays(6);

        donation(day, 2);
        donation(day, 3);

        DailyReport report = reportService.summaryFor(day);
        assertThat(report.getDonations()).isEqualTo(2);
        assertThat(report.getUnitsCollected()).isEqualTo(5);
    }

    /** A day nothing happened on is left out of the list rather than listed as a row of zeros. */
    @Test
    void quietDaysAreLeftOutOfTheList() {
        LocalDate day = LocalDate.now().minusDays(7);
        donation(day, 1);

        List<DailyReport> reports = reportService.recentDays();

        assertThat(reports).hasSize(1);
        assertThat(reports.get(0).getDate()).isEqualTo(day);
    }

    @Test
    void theListIsNewestFirst() {
        LocalDate older = LocalDate.now().minusDays(9);
        LocalDate newer = LocalDate.now().minusDays(2);
        donation(older, 1);
        donation(newer, 1);

        List<DailyReport> reports = reportService.recentDays();

        assertThat(reports).hasSize(2);
        assertThat(reports.get(0).getDate()).isEqualTo(newer);
        assertThat(reports.get(1).getDate()).isEqualTo(older);
    }

    /**
     * The list covers 30 days, but a detail page is reachable for any date - so an older day is
     * absent from the list yet still opens with its real figures.
     */
    @Test
    void aDayOutsideTheWindowIsNotListedButStillOpens() {
        LocalDate longAgo = LocalDate.now().minusDays(60);
        donation(longAgo, 2);

        assertThat(reportService.recentDays()).isEmpty();

        DailyReport report = reportService.summaryFor(longAgo);
        assertThat(report.getDonations()).isEqualTo(1);
        assertThat(report.getUnitsCollected()).isEqualTo(2);
    }

    @Test
    void aDayWithNothingOnItReportsItselfAsEmpty() {
        assertThat(reportService.summaryFor(LocalDate.now().minusDays(11)).isEmpty()).isTrue();
    }

    /** The detail page lists the rows behind the figures, not just the counts. */
    @Test
    void theRowsBehindEachFigureAreReturnedForTheDay() {
        LocalDate day = LocalDate.now().minusDays(8);

        request(day.atTime(9, 0), null, RequestStatus.PENDING);
        request(LocalDate.now().minusDays(20).atTime(9, 0), day.atTime(14, 0), RequestStatus.APPROVED);
        donation(day, 1);

        assertThat(reportService.requestsRaisedOn(day)).hasSize(1);
        assertThat(reportService.requestsDecidedOn(day)).hasSize(1);
        assertThat(reportService.donationsOn(day)).hasSize(1);
    }
}
