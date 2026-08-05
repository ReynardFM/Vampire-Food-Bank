package com.project.BloodBank.service;

import com.project.BloodBank.dto.DonationRecordDto;
import com.project.BloodBank.exception.ResourceNotFoundException;
import com.project.BloodBank.model.Donation;
import com.project.BloodBank.model.DonationRequest;
import com.project.BloodBank.model.User;
import com.project.BloodBank.model.enums.BloodGroup;
import com.project.BloodBank.model.enums.RequestStatus;
import com.project.BloodBank.repository.DonationRepository;
import com.project.BloodBank.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

// Recording and reading donations. The rules live here, not in the controller, so they hold for
// every caller including the tests.
@Service
public class DonationService {

    // Final fields set by the constructor: Spring injects them, no @Autowired needed.
    private final DonationRepository donationRepository;
    private final UserRepository userRepository;
    private final DonationRequestService donationRequestService;

    public DonationService(DonationRepository donationRepository,
                           UserRepository userRepository,
                           DonationRequestService donationRequestService) {
        this.donationRepository = donationRepository;
        this.userRepository = userRepository;
        this.donationRequestService = donationRequestService;
    }

    // Records a collected donation and closes out the request it was for.
    // @Transactional covers all three tables written below, so a failure part way leaves nothing.
    @Transactional
    public Donation recordDonation(DonationRecordDto dto, User donor) {

        // Copy the form values across. A DTO rather than the entity, so a crafted request cannot
        // set fields the form never offered.
        Donation donation = new Donation();
        donation.setDonor(donor);
        donation.setDonationDate(dto.getDonationDate());
        donation.setUnitsDonated(dto.getUnitsDonated());
        donation.setLocation(dto.getLocation());

        // Linking is optional; a walk-in donation leaves this null.
        if (dto.getLinkedRequestId() != null) {
            DonationRequest linkedRequest = donationRequestService.getRequestById(dto.getLinkedRequestId());

            // Rejected or already fulfilled means finished. Do not reopen it.
            if (linkedRequest.getStatus() != RequestStatus.APPROVED) {
                throw new IllegalStateException("That request is no longer approved, so it cannot be linked.");
            }

            requireNotOwnRequest(donor, linkedRequest);
            requireCompatible(donor, linkedRequest);
            donation.setLinkedRequest(linkedRequest);
        }

        Donation savedDonation = donationRepository.save(donation);

        // Only ever move this forward. Donations are often entered late, so the date being saved is
        // not necessarily the newest on record, and assigning blindly would make a recent donor
        // look overdue.
        LocalDate previous = donor.getLastDonationDate();
        if (previous == null || dto.getDonationDate().isAfter(previous)) {
            donor.setLastDonationDate(dto.getDonationDate());
            userRepository.save(donor);
        }

        // Where the two halves of the app meet: recording a donation is the only thing that marks a
        // request FULFILLED - and only once enough blood has actually been collected for it.
        if (donation.getLinkedRequest() != null) {
            closeIfFullyCollected(donation.getLinkedRequest());
        }

        return savedDonation;
    }

    // Marks a request fulfilled once the units collected for it reach what was asked for, and
    // leaves it approved otherwise.
    //
    // A request can ask for several units while one person gives one, so a single donation is often
    // only part of the answer. Closing the request on the first one recorded a five-unit request as
    // served by a single unit, and dropped it out of the queue with the patient still needing blood.
    //
    // The sum includes the donation just saved. Hibernate flushes pending writes before running a
    // query, so the row is already in the database by the time this counts it.
    private void closeIfFullyCollected(DonationRequest request) {
        if (collectedFor(request.getId()) >= request.getUnitsNeeded()) {
            donationRequestService.markAsFulfilled(request.getId());
        }
    }

    // Units collected against a request so far. Also read by the screens that show progress.
    @Transactional(readOnly = true)
    public int collectedFor(Long requestId) {
        Long collected = donationRepository.sumUnitsCollectedFor(requestId);
        return collected != null ? collected.intValue() : 0;
    }

    // Refuses a donation recorded against the donor's own request.
    //
    // Somebody who needs blood is not in a position to give it, so a request being closed out by
    // the person who raised it is either a mistake or a way of quietly clearing the queue. Either
    // way the request would be marked served while the patient still needs blood.
    //
    // Checked here rather than only hidden in the form, because the request id is posted from the
    // browser and the recording URL can be reached directly.
    private void requireNotOwnRequest(User donor, DonationRequest request) {
        if (request.getRequestedBy().getId().equals(donor.getId())) {
            throw new IllegalStateException(
                    "A request cannot be fulfilled by the person who raised it.");
        }
    }

    // Refuses a link whose patient could not safely receive this blood.
    // The dropdown already filters, but the request id is posted from the browser, so it is checked
    // again here. Without this an A+ donation could mark an O- request fulfilled.
    private void requireCompatible(User donor, DonationRequest request) {
        BloodGroup donorGroup = donor.getBloodGroup();

        // Ordinary state for a new donor: registration never asks for a blood group.
        if (donorGroup == null) {
            throw new IllegalStateException(
                    "This donor has no blood group on file, so their donation cannot be linked to a request.");
        }

        // compatibleDonors() lists everyone the patient can accept, so this donor must be in it.
        if (!request.getRequestedBloodGroup().compatibleDonors().contains(donorGroup)) {
            throw new IllegalStateException(
                    donorGroup.getDisplayName() + " blood cannot be given to a patient needing "
                            + request.getRequestedBloodGroup().getDisplayName() + ".");
        }
    }

    // readOnly lets Hibernate skip change detection, and makes an accidental write fail loudly.
    @Transactional(readOnly = true)
    public Page<Donation> getDonationHistory(User donor, Pageable pageable) {
        return donationRepository.findByDonor(donor, pageable);
    }

    // Turns the empty Optional into an exception once, so callers can treat the result as real.
    @Transactional(readOnly = true)
    public Donation getDonationById(Long id) {
        return donationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Donation not found: " + id));
    }
}
