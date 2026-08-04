package com.project.BloodBank.service;

import com.project.BloodBank.dto.DonationRecordDto;
import com.project.BloodBank.exception.ResourceNotFoundException;
import com.project.BloodBank.model.Donation;
import com.project.BloodBank.model.DonationRequest;
import com.project.BloodBank.model.User;
import com.project.BloodBank.model.enums.RequestStatus;
import com.project.BloodBank.repository.DonationRepository;
import com.project.BloodBank.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DonationService {

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

    @Transactional
    public Donation recordDonation(DonationRecordDto dto, User donor) {
        Donation donation = new Donation();
        donation.setDonor(donor);
        donation.setDonationDate(dto.getDonationDate());
        donation.setUnitsDonated(dto.getUnitsDonated());
        donation.setLocation(dto.getLocation());

        if (dto.getLinkedRequestId() != null) {
            DonationRequest linkedRequest = donationRequestService.getRequestById(dto.getLinkedRequestId());

            if (linkedRequest.getStatus() != RequestStatus.APPROVED) {
                throw new IllegalStateException(
                        "Donation can only be linked to an APPROVED request. Current status: " + linkedRequest.getStatus());
            }

            donation.setLinkedRequest(linkedRequest);
        }

        Donation savedDonation = donationRepository.save(donation);

        donor.setLastDonationDate(dto.getDonationDate());
        userRepository.save(donor);

        if (donation.getLinkedRequest() != null) {
            donationRequestService.markAsFulfilled(donation.getLinkedRequest().getId());
        }

        return savedDonation;
    }

    @Transactional(readOnly = true)
    public Page<Donation> getDonationHistory(User donor, Pageable pageable) {
        return donationRepository.findByDonor(donor, pageable);
    }

    @Transactional(readOnly = true)
    public Donation getDonationById(Long id) {
        return donationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Donation not found: " + id));
    }
}
