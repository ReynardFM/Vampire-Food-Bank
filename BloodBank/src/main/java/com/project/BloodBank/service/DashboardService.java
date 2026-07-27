package com.project.BloodBank.service;

import com.project.BloodBank.model.enums.BloodGroup;
import com.project.BloodBank.model.enums.RequestStatus;
import com.project.BloodBank.model.enums.Role;
import com.project.BloodBank.repository.DonationRepository;
import com.project.BloodBank.repository.DonationRequestRepository;
import com.project.BloodBank.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.Map;

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

    @Transactional(readOnly = true)
    public long getTotalActiveDonors() {
        return userRepository.countByActiveTrueAndRole(Role.DONOR);
    }

    @Transactional(readOnly = true)
    public long getDonationsThisMonth() {
        return donationRepository.countDonationsThisMonth();
    }

    @Transactional(readOnly = true)
    public Map<BloodGroup, Long> getDonationsByBloodGroup() {
        Map<BloodGroup, Long> result = new EnumMap<>(BloodGroup.class);

        for (Object[] row : donationRepository.countGroupedByDonorBloodGroup()) {
            BloodGroup group = (BloodGroup) row[0];
            Long count = (Long) row[1];

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

    @Transactional(readOnly = true)
    public long getPendingRequestCount() {
        return donationRequestRepository.countByStatus(RequestStatus.PENDING);
    }
}