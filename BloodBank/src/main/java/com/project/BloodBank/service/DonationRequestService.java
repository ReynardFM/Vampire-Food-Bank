package com.project.BloodBank.service;

import com.project.BloodBank.dto.DonationRequestDto;
import com.project.BloodBank.exception.ResourceNotFoundException;
import com.project.BloodBank.model.DonationRequest;
import com.project.BloodBank.model.User;
import com.project.BloodBank.model.enums.RequestStatus;
import com.project.BloodBank.repository.DonationRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DonationRequestService {

    private final DonationRequestRepository donationRequestRepository;

    public DonationRequestService(DonationRequestRepository donationRequestRepository) {
        this.donationRequestRepository = donationRequestRepository;
    }

    @Transactional
    public DonationRequest createRequest(DonationRequestDto dto, User requester) {
        DonationRequest request = new DonationRequest();
        request.setRequestedBloodGroup(dto.getRequestedBloodGroup());
        request.setUnitsNeeded(dto.getUnitsNeeded());
        request.setHospitalName(dto.getHospitalName());
        request.setHospitalAddress(dto.getHospitalAddress());
        request.setUrgencyLevel(dto.getUrgencyLevel());
        request.setNotes(dto.getNotes());
        request.setRequestedBy(requester);

        return donationRequestRepository.save(request);
    }

    @Transactional(readOnly = true)
    public List<DonationRequest> getRequestsByUser(User user) {
        return donationRequestRepository.findByRequestedBy(user);
    }

    @Transactional(readOnly = true)
    public List<DonationRequest> getPendingRequests() {
        return donationRequestRepository.findByStatus(RequestStatus.PENDING);
    }

    @Transactional(readOnly = true)
    public DonationRequest getRequestById(Long id) {
        return donationRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Donation request not found: " + id));
    }

    @Transactional
    public DonationRequest approveRequest(Long id) {
        DonationRequest request = getRequestById(id);

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new IllegalStateException("Only PENDING requests can be approved. Current status: " + request.getStatus());
        }

        request.setStatus(RequestStatus.APPROVED);
        return donationRequestRepository.save(request);
    }

    @Transactional
    public DonationRequest rejectRequest(Long id) {
        DonationRequest request = getRequestById(id);

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new IllegalStateException("Only PENDING requests can be rejected. Current status: " + request.getStatus());
        }

        request.setStatus(RequestStatus.REJECTED);
        return donationRequestRepository.save(request);
    }

    @Transactional
    public DonationRequest markAsFulfilled(Long id) {
        DonationRequest request = getRequestById(id);
        request.setStatus(RequestStatus.FULFILLED);
        return donationRequestRepository.save(request);
    }
}

