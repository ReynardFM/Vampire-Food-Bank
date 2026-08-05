package com.project.BloodBank.service;

import com.project.BloodBank.model.enums.BloodGroup;
import com.project.BloodBank.model.enums.RequestStatus;
import com.project.BloodBank.model.enums.Role;
import com.project.BloodBank.repository.DonationRepository;
import com.project.BloodBank.repository.DonationRequestRepository;
import com.project.BloodBank.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Map;

// The numbers behind the admin dashboard.
//
// Every method here counts in the database rather than loading rows and counting in Java, so the
// dashboard costs a handful of small queries no matter how much history exists.
//
// The "today" methods all call LocalDate.now() at the moment they run, not at startup, so the
// figures follow the calendar without the application being restarted.
@Service
public class DashboardService {

    private final UserRepository userRepository;
    private final DonationRepository donationRepository;
    private final DonationRequestRepository donationRequestRepository;

    public DashboardService(UserRepository userRepository,
                            DonationRepository donationRepository,
                            DonationRequestRepository donationRequestRepository) {
        this.userRepository = userRepository;
        this.donationRepository = donationRepository;
        this.donationRequestRepository = donationRequestRepository;
    }

    // --- Headline figures ---

    @Transactional(readOnly = true)
    public long getTotalActiveDonors() {
        return userRepository.countByActiveTrueAndRole(Role.DONOR);
    }

    @Transactional(readOnly = true)
    public long getDonationsThisMonth() {
        return donationRepository.countDonationsThisMonth();
    }

    @Transactional(readOnly = true)
    public long getPendingRequestCount() {
        return donationRequestRepository.countByStatus(RequestStatus.PENDING);
    }

    // atStartOfDay() gives midnight, so "today" means since midnight rather than the last 24 hours.
    @Transactional(readOnly = true)
    public long getRequestsRaisedToday() {
        return donationRequestRepository.countRaisedSince(LocalDate.now().atStartOfDay());
    }

    @Transactional(readOnly = true)
    public long getRequestsDecidedToday() {
        return donationRequestRepository.countDecidedSince(LocalDate.now().atStartOfDay());
    }

    // --- Chart data ---
    // These four turn the repositories' Object[] rows into maps the controller can read. Each row
    // holds two values: the group or status, then its count.
    //
    // EnumMap rather than HashMap because the keys are enum constants - it is backed by a plain
    // array indexed by ordinal, and iterates in declaration order, so the bars always come out in
    // the same sequence.

    @Transactional(readOnly = true)
    public Map<BloodGroup, Long> getDonationsByBloodGroup() {
        Map<BloodGroup, Long> result = new EnumMap<>(BloodGroup.class);

        for (Object[] row : donationRepository.countGroupedByDonorBloodGroup()) {
            BloodGroup group = (BloodGroup) row[0];
            Long count = (Long) row[1];

            // Donors who never completed their profile group under a null key. EnumMap rejects
            // null keys outright, so they have to be dropped rather than merely ignored.
            if (group != null) {
                result.put(group, count);
            }
        }

        return result;
    }

    @Transactional(readOnly = true)
    public Map<RequestStatus, Long> getRequestStatusCounts() {
        Map<RequestStatus, Long> result = new EnumMap<>(RequestStatus.class);

        for (Object[] row : donationRequestRepository.countGroupedByStatus()) {
            RequestStatus status = (RequestStatus) row[0];
            Long count = (Long) row[1];
            result.put(status, count);
        }

        return result;
    }

    // The share of each status bar that moved there today.
    @Transactional(readOnly = true)
    public Map<RequestStatus, Long> getRequestStatusCountsToday() {
        Map<RequestStatus, Long> result = new EnumMap<>(RequestStatus.class);

        for (Object[] row : donationRequestRepository
                .countGroupedByStatusChangedSince(LocalDate.now().atStartOfDay())) {
            result.put((RequestStatus) row[0], (Long) row[1]);
        }

        return result;
    }

    // The share of each blood group's donations recorded today. A LocalDate here rather than a
    // LocalDateTime, because donationDate is a date with no time attached.
    @Transactional(readOnly = true)
    public Map<BloodGroup, Long> getDonationsByBloodGroupToday() {
        Map<BloodGroup, Long> result = new EnumMap<>(BloodGroup.class);

        for (Object[] row : donationRepository.countGroupedByDonorBloodGroupSince(LocalDate.now())) {
            BloodGroup group = (BloodGroup) row[0];
            if (group != null) {
                result.put(group, (Long) row[1]);
            }
        }

        return result;
    }
}
