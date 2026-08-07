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

    // Two people, because a request cannot be fulfilled by whoever raised it. Every request built
    // by the helpers below belongs to the requester, so the donor is always a third party - which
    // is the ordinary case, and keeps the self-donation rule from firing in tests about other things.
    private User requester;
    private User admin;

    @BeforeEach
    void setUp() {
        donor = userRepository.save(
                person("donor-history@test.local", "History Donor", BloodGroup.A_POSITIVE));
        requester = userRepository.save(
                person("requester@test.local", "Request Raiser", BloodGroup.A_POSITIVE));
        // Whoever enters a donation. Recorded against any request the donation closes.
        admin = userRepository.save(
                person("recorder@test.local", "Recording Admin", BloodGroup.O_NEGATIVE));
    }

    private User person(String email, String name, BloodGroup group) {
        User user = new User();
        user.setEmail(email);
        user.setFullName(name);
        user.setPassword(passwordEncoder.encode("Password123!"));
        user.setRole(Role.USER);
        user.setActive(true);
        user.setBloodGroup(group);
        return user;
    }

    private DonationRequest request(RequestStatus status) {
        return request(status, BloodGroup.A_POSITIVE);
    }

    private DonationRequest request(RequestStatus status, int unitsNeeded) {
        return request(status, BloodGroup.A_POSITIVE, unitsNeeded);
    }

    private DonationRequest request(RequestStatus status, BloodGroup requestedGroup) {
        return request(status, requestedGroup, 1);
    }

    private DonationRequest request(RequestStatus status, BloodGroup requestedGroup, int unitsNeeded) {
        return request(status, requestedGroup, unitsNeeded, requester);
    }

    private DonationRequest request(RequestStatus status, BloodGroup requestedGroup,
                                    int unitsNeeded, User raisedBy) {
        DonationRequest request = new DonationRequest();
        request.setRequestedBy(raisedBy);
        request.setRequestedBloodGroup(requestedGroup);
        request.setUnitsNeeded(unitsNeeded);
        request.setHospitalName("Test Hospital");
        request.setUrgencyLevel(UrgencyLevel.HIGH);
        request.setStatus(status);
        return requestRepository.save(request);
    }

    private DonationRecordDto dto(LocalDate on, Long linkedRequestId) {
        return dto(on, linkedRequestId, 1);
    }

    private DonationRecordDto dto(LocalDate on, Long linkedRequestId, int units) {
        DonationRecordDto dto = new DonationRecordDto();
        dto.setDonationDate(on);
        dto.setUnitsDonated(units);
        dto.setLocation("Test Hospital");
        dto.setLinkedRequestId(linkedRequestId);
        return dto;
    }

    @Test
    void recordingAnUnlinkedDonationUpdatesTheDonorsLastDonationDate() {
        LocalDate when = LocalDate.now().minusDays(3);

        Donation saved = donationService.recordDonation(dto(when, null), donor, admin);

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

        donationService.recordDonation(dto(LocalDate.now(), approved.getId()), donor, admin);

        assertThat(requestRepository.findById(approved.getId()))
                .get()
                .extracting(DonationRequest::getStatus)
                .isEqualTo(RequestStatus.FULFILLED);
    }

    @Test
    void linkingToARequestThatIsNotApprovedIsRefused() {
        DonationRequest pending = request(RequestStatus.PENDING);

        assertThatThrownBy(() ->
                donationService.recordDonation(dto(LocalDate.now(), pending.getId()), donor, admin))
                .isInstanceOf(IllegalStateException.class);

        assertThat(requestRepository.findById(pending.getId()))
                .get()
                .extracting(DonationRequest::getStatus)
                .isEqualTo(RequestStatus.PENDING);
    }

    /**
     * Donations are often entered days after collection, so a later entry may carry an earlier
     * date. lastDonationDate has to stay the most recent one.
     */
    @Test
    void aBackdatedDonationDoesNotDragTheLastDonationDateBackwards() {
        LocalDate recent = LocalDate.now().minusDays(2);
        donationService.recordDonation(dto(recent, null), donor, admin);

        donationService.recordDonation(dto(LocalDate.now().minusMonths(6), null), donor, admin);

        assertThat(userRepository.findById(donor.getId()))
                .get()
                .extracting(User::getLastDonationDate)
                .isEqualTo(recent);
    }

    /** An A+ donation must not be able to close out a request raised for an O- patient. */
    @Test
    void linkingToARequestThisDonorCannotServeIsRefused() {
        DonationRequest incompatible = request(RequestStatus.APPROVED, BloodGroup.O_NEGATIVE);

        assertThatThrownBy(() ->
                donationService.recordDonation(dto(LocalDate.now(), incompatible.getId()), donor, admin))
                .isInstanceOf(IllegalStateException.class);

        assertThat(requestRepository.findById(incompatible.getId()))
                .get()
                .extracting(DonationRequest::getStatus)
                .isEqualTo(RequestStatus.APPROVED);
    }

    /**
     * A request can ask for more units than one person gives. Closing it on the first donation
     * recorded a five-unit request as served by a single unit, with the patient still needing blood.
     */
    @Test
    void aPartialDonationLeavesTheRequestApproved() {
        DonationRequest approved = request(RequestStatus.APPROVED, 5);

        donationService.recordDonation(dto(LocalDate.now(), approved.getId(), 2), donor, admin);

        assertThat(requestRepository.findById(approved.getId()))
                .get()
                .extracting(DonationRequest::getStatus)
                .isEqualTo(RequestStatus.APPROVED);
        assertThat(donationService.collectedFor(approved.getId())).isEqualTo(2);
    }

    @Test
    void theDonationThatCompletesARequestFulfilsIt() {
        DonationRequest approved = request(RequestStatus.APPROVED, 3);

        donationService.recordDonation(dto(LocalDate.now(), approved.getId(), 2), donor, admin);
        donationService.recordDonation(dto(LocalDate.now(), approved.getId(), 1), donor, admin);

        assertThat(requestRepository.findById(approved.getId()))
                .get()
                .extracting(DonationRequest::getStatus)
                .isEqualTo(RequestStatus.FULFILLED);
    }

    /** Over-collecting still closes the request rather than leaving it open forever. */
    @Test
    void collectingMoreThanNeededStillFulfilsTheRequest() {
        DonationRequest approved = request(RequestStatus.APPROVED, 2);

        donationService.recordDonation(dto(LocalDate.now(), approved.getId(), 3), donor, admin);

        assertThat(requestRepository.findById(approved.getId()))
                .get()
                .extracting(DonationRequest::getStatus)
                .isEqualTo(RequestStatus.FULFILLED);
    }

    /**
     * Somebody who needs blood is not in a position to give it. Allowing this would close a request
     * as served while the patient still needed blood.
     */
    @Test
    void aRequestCannotBeFulfilledByThePersonWhoRaisedIt() {
        // Raised by the donor themselves, unlike every other request in this class.
        DonationRequest own = request(RequestStatus.APPROVED, BloodGroup.A_POSITIVE, 1, donor);

        assertThatThrownBy(() ->
                donationService.recordDonation(dto(LocalDate.now(), own.getId()), donor, admin))
                .isInstanceOf(IllegalStateException.class);

        assertThat(requestRepository.findById(own.getId()))
                .get()
                .extracting(DonationRequest::getStatus)
                .isEqualTo(RequestStatus.APPROVED);
    }

    @Test
    void aDonorWithNoBloodGroupCannotLinkToAnyRequest() {
        donor.setBloodGroup(null);
        donor = userRepository.save(donor);
        DonationRequest approved = request(RequestStatus.APPROVED);

        assertThatThrownBy(() ->
                donationService.recordDonation(dto(LocalDate.now(), approved.getId()), donor, admin))
                .isInstanceOf(IllegalStateException.class);
    }
}
