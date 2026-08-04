package com.project.BloodBank.service;

import com.project.BloodBank.dto.DonationRecordDto;
import com.project.BloodBank.model.Donation;
import com.project.BloodBank.model.DonationRequest;
import com.project.BloodBank.model.User;
import com.project.BloodBank.model.enums.BloodGroup;
import com.project.BloodBank.model.enums.RequestStatus;
import com.project.BloodBank.model.enums.Role;
import com.project.BloodBank.model.enums.UrgencyLevel;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DonationServiceTest {

    @Autowired
    private DonationService donationService;

    @Autowired
    private DonationRequestRepository requestRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User donor;

    @BeforeEach
    void setUp() {
        donor = new User();
        donor.setEmail("donor-history@test.local");
        donor.setFullName("History Donor");
        donor.setPassword(passwordEncoder.encode("Password123!"));
        donor.setRole(Role.DONOR);
        donor.setActive(true);
        donor.setBloodGroup(BloodGroup.A_POSITIVE);
        donor = userRepository.save(donor);
    }

    private DonationRequest request(RequestStatus status) {
        DonationRequest request = new DonationRequest();
        request.setRequestedBy(donor);
        request.setRequestedBloodGroup(BloodGroup.A_POSITIVE);
        request.setUnitsNeeded(1);
        request.setHospitalName("Test Hospital");
        request.setUrgencyLevel(UrgencyLevel.HIGH);
        request.setStatus(status);
        return requestRepository.save(request);
    }

    private DonationRecordDto dto(LocalDate on, Long linkedRequestId) {
        DonationRecordDto dto = new DonationRecordDto();
        dto.setDonationDate(on);
        dto.setUnitsDonated(1);
        dto.setLocation("Test Hospital");
        dto.setLinkedRequestId(linkedRequestId);
        return dto;
    }

    @Test
    void recordingAnUnlinkedDonationUpdatesTheDonorsLastDonationDate() {
        LocalDate when = LocalDate.now().minusDays(3);

        Donation saved = donationService.recordDonation(dto(when, null), donor);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getLinkedRequest()).isNull();
        assertThat(userRepository.findById(donor.getId()))
                .get()
                .extracting(User::getLastDonationDate)
                .isEqualTo(when);
    }

    @Test
    void linkingToAnApprovedRequestMarksItFulfilled() {
        DonationRequest approved = request(RequestStatus.APPROVED);

        donationService.recordDonation(dto(LocalDate.now(), approved.getId()), donor);

        assertThat(requestRepository.findById(approved.getId()))
                .get()
                .extracting(DonationRequest::getStatus)
                .isEqualTo(RequestStatus.FULFILLED);
    }

    @Test
    void linkingToARequestThatIsNotApprovedIsRefused() {
        DonationRequest pending = request(RequestStatus.PENDING);

        assertThatThrownBy(() ->
                donationService.recordDonation(dto(LocalDate.now(), pending.getId()), donor))
                .isInstanceOf(IllegalStateException.class);

        assertThat(requestRepository.findById(pending.getId()))
                .get()
                .extracting(DonationRequest::getStatus)
                .isEqualTo(RequestStatus.PENDING);
    }
}
